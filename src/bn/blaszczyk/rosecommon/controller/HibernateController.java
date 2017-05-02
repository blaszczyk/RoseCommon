
package bn.blaszczyk.rosecommon.controller;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.rosecommon.RoseException;
import bn.blaszczyk.rosecommon.tools.EntityUtils;
import bn.blaszczyk.rosecommon.tools.TypeManager;

import static bn.blaszczyk.rosecommon.tools.Preferences.*;

public class HibernateController implements ModelController {
	
	private static final Logger LOGGER = Logger.getLogger(HibernateController.class);
	private static final Calendar calendar = Calendar.getInstance();

	private static final String KEY_URL = "hibernate.connection.url";
	private static final String KEY_USER = "hibernate.connection.username";
	private static final String KEY_PW = "hibernate.connection.password";

	private static final String TIMESTAMP = "timestamp";
	
	private SessionFactory sessionFactory;
	private Session session;

	public HibernateController()
	{
		configureDataBase();
	}

	@Override
	public <T extends Readable> List<T> getEntities(Class<T> type) throws RoseException
	{
		try
		{
			LOGGER.debug("start load" + " " + type.getSimpleName());
			Session session = getSession();
			synchronized (session)
			{
				Criteria criteria = session.createCriteria(type);

				int fetchTimeSpan = getIntegerValue(FETCH_TIMESPAN, Integer.MAX_VALUE);
				if(fetchTimeSpan != Integer.MAX_VALUE)
				{
					calendar.setTime(new Date());
					calendar.add(Calendar.DATE, - fetchTimeSpan);
					criteria.add( Expression.ge(TIMESTAMP,calendar.getTime()));
					LOGGER.debug("fetch entity age restriction: " + fetchTimeSpan + " days");
				}
				
				List<?> list = criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
				LOGGER.debug("end load entities: " + type.getName() + " count=" + list.size());
				return list.stream().map(type::cast).collect(Collectors.toList());
				
			}
		}
		catch(HibernateException e)
		{
			throw new RoseException("error loading entities: " + type.getName(), e);
		}
	}
	
	@Override
	public List<Integer> getIds(final Class<? extends Readable> type) throws RoseException
	{
		final Session session = getSession();
		final String message = "fetching all ids of" + type.getSimpleName();
		try
		{
			synchronized (session)
			{
				LOGGER.debug("start " + message);
				final Criteria criteria = session.createCriteria(type);
				criteria.setProjection(Projections.property("id"));
				final int fetchTimeSpan = getIntegerValue(FETCH_TIMESPAN, Integer.MAX_VALUE);
				if(fetchTimeSpan != Integer.MAX_VALUE)
				{
					calendar.setTime(new Date());
					calendar.add(Calendar.DATE, - fetchTimeSpan);
					criteria.add( Expression.ge(TIMESTAMP,calendar.getTime()));
				}				
				final List<?> list = criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
				LOGGER.debug("end " + message + " count=" + list.size());
				return list.stream().map(Integer.class::cast).collect(Collectors.toList());
			}
		}
		catch(HibernateException e)
		{
			throw new RoseException("error " + message, e);
		}
	}

	@Override
	public int getEntityCount(final Class<? extends Readable> type) throws RoseException
	{
		final Session session = getSession();
		final String message = "fetching count for " + type.getSimpleName();
		try
		{
			synchronized (session)
			{
				LOGGER.debug("start " + message);
				final Object oCount = session.createCriteria(type)
										.setProjection(Projections.rowCount())
										.uniqueResult();
				LOGGER.debug("end " + message + " count= " + oCount);
				if(oCount instanceof Number)
					return ((Number)oCount).intValue();
				throw new RoseException("No Number instance: " + oCount);
			}
		}
		catch (HibernateException e) 
		{
			throw RoseException.wrap(e, "error getting count for " + type.getSimpleName());
		}
	}

	@Override
	public <T extends Readable> T getEntityById(final Class<T> type, final int id) throws RoseException
	{
		final Session session = getSession();
		final String message = "fetching " + type.getSimpleName() + " id=" + id;
		try
		{
			synchronized (session)
			{
				LOGGER.debug("start " + message);
				final Criteria criteria = session.createCriteria(type);
				criteria.add(Restrictions.idEq(id));
				final T result = type.cast(criteria.uniqueResult());
				LOGGER.debug("end " + message);
				return result;
			}
		}
		catch (HibernateException e) 
		{
			throw RoseException.wrap(e, "error getting " + type.getSimpleName() + " with id=" + id);
		}
	}
	
	@Override
	public <T extends Readable> List<T> getEntitiesByIds(final Class<T> type, List<Integer> ids)
			throws RoseException
	{
		final Session session = getSession();
		final String message = "fetching " + type.getSimpleName() + " ids=" + ids;
		try
		{
			synchronized (session)
			{
				LOGGER.debug("start " + message);
				final Criteria criteria = session.createCriteria(type);
				criteria.add(Restrictions.in("id", ids));
				final List<?> list = criteria.list();
				final List<T> entities = list.stream().map(type::cast).collect(Collectors.toList());
				LOGGER.debug("start " + message);
				return entities;
			}
		}
		catch (HibernateException e) 
		{
			throw RoseException.wrap(e, "error getting " + type.getSimpleName() + " with ids=" + ids);
		}
	}

	@Override
	public void update(Writable... entities) throws RoseException
	{
		final Session session = getSession();
		try
		{
			synchronized (session)
			{
				LOGGER.debug("start update");
				final Transaction transaction = session.beginTransaction();
				for(Writable entity : entities)
				{
					if(entity == null)
						continue;
					if(entity.getId() < 0)
					{
						LOGGER.warn("saving new entity:\r\n" + EntityUtils.toStringFull(entity));
						final Integer id = (Integer) session.save(entity);
						entity.setId(id);
					}
					else
					{
						LOGGER.debug("updating entity:\r\n" + EntityUtils.toStringFull(entity));
						session.update(entity);
					}
				}
				transaction.commit();
				LOGGER.debug("end update");
			}
		}
		catch(HibernateException e)
		{
			throw new RoseException("error saving or updating entities to database",e);
		}
	}

	@Override
	public void delete(final Writable entity) throws RoseException
	{
		if(entity == null)
			return;
		LOGGER.warn("start deleting entity:\r\n" + EntityUtils.toStringFull(entity));
		final Session sesson = getSession();
		final Set<Writable> changedEntities = new LinkedHashSet<>();
		synchronized (sesson)
		{
			for(int i = 0; i < entity.getEntityCount(); i++)
			{
				if(entity.getRelationType(i).isSecondMany())
					for(final Readable subEntity : entity.getEntityValueMany(i))
						if(subEntity != null)
							changedEntities.add((Writable) subEntity);
				else
					if(entity.getEntityValueOne(i) != null)
						changedEntities.add((Writable) entity.getEntityValueOne(i));
			}
			update(changedEntities.toArray(new Writable[changedEntities.size()]));
			try
			{
				sesson.beginTransaction();
				sesson.delete(entity);
				sesson.getTransaction().commit();
				LOGGER.debug("end deleting entity: " + EntityUtils.toStringSimple(entity));
			}
			catch(HibernateException e)
			{
				throw new RoseException("error deleting " + entity, e);
			}			
		}
	}

	@Override
	public <T extends Readable> T createNew(final Class<T> type) throws RoseException
	{
		final T entity = TypeManager.newInstance(type);
		final Session session = getSession();
		synchronized (session)
		{
			LOGGER.debug("start creating " + type.getSimpleName());
			session.beginTransaction();
			entity.setId((Integer) session.save(entity));
			session.getTransaction().commit();
		}
		LOGGER.debug("end creating: " + EntityUtils.toStringPrimitives(entity));
		return entity;
	}
	
	@Override
	public Writable createCopy(final Writable entity) throws RoseException
	{
		final Writable copy = createNew(entity.getClass());
		for(int i = 0; i < copy.getFieldCount(); i++)
			copy.setField( i, copy.getFieldValue(i));
		for(int i = 0; i < copy.getEntityCount(); i++)
			switch(copy.getRelationType(i))
			{
			case ONETOONE:
//				Writable subCopy = createCopy( (Writable) copy.getEntityValue(i) );
//				copy.setEntity( i, subCopy );
				break;
			case ONETOMANY:
				for( Readable o : copy.getEntityValueMany(i) )
					copy.addEntity( i, createCopy((Writable) o));
				break;
			case MANYTOONE:
				copy.setEntity(i, (Writable) copy.getEntityValueOne(i));
				break;
			case MANYTOMANY:
				break;
			}
		return copy;
	}
	
	@Override
	public void close()
	{
		if(session != null && !session.isOpen())
		{
			LOGGER.debug("closing session " + session);
			synchronized (session)
			{
				session.close();
			}
			LOGGER.debug("session closed");
		}
		session = null;
	}
	
	public List<?> listQuery( final String query ) throws RoseException
	{
		final Session session = getSession();
		try
		{
			synchronized (session)
			{
				LOGGER.debug("start query \"" + query + "\"");
				final SQLQuery sqlQuery = session.createSQLQuery(query);
				final List<?> list = sqlQuery.setResultTransformer(Criteria.ROOT_ENTITY).list();
				if(list == null)
					return Collections.emptyList();
				return list;
			}
		}
		catch(HibernateException e)
		{
			throw new RoseException("Unable to execute query '" + query + "'", e);
		}
	}

	//TODO: refactor to something useful
//	private void checkConnection()
//	{
//		if(sessionLocked())
//			return;
//		String message;
//		boolean wasConnected = connected;
//		if(session instanceof SessionImpl)
//		{
//			try
//			{
//				connected = ((SessionImpl)session).connection().isValid(10);
//			}
//			catch (HibernateException | SQLException e1)
//			{
//				if(wasConnected)
//				{
//					LOGGER.error("no connection to " + dbFullUrl, e1);
//				}
//				connected = false;
//			}
//			message =  connected ? "connected" : "disconnected" ;
//		}
//		else
//			message = "unknown";
//		LOGGER.debug(dbMessage + " - " + message);
//	}

	private void configureDataBase()
	{
		String dburl = getStringValue(DB_HOST,null);
		String dbport = getStringValue(DB_PORT,null);
		String dbname = getStringValue(DB_NAME,null);
		String dbuser = getStringValue(DB_USER,null);
		String dbpassword = getStringValue(DB_PASSWORD,null);

		Configuration configuration = new AnnotationConfiguration().configure();
		if(dburl != null && dbport != null && dbname != null)
			configuration.setProperty(KEY_URL, String.format("jdbc:mysql://%s:%s/%s",dburl,dbport,dbname));
		if(dbuser != null)
			configuration.setProperty(KEY_USER, dbuser);
		if(dbpassword != null)
			configuration.setProperty(KEY_PW, dbpassword);
		sessionFactory = configuration.buildSessionFactory();
	}

	private Session getSession() throws RoseException
	{
		if(session == null || !session.isOpen())
		{
			LOGGER.debug("opening session");
			try
			{
				session = sessionFactory.openSession();
				LOGGER.info("session open");
			}
			catch (HibernateException e) 
			{
				throw new RoseException("no open session", e);
			}
		}
		return session;
	}
	
}
