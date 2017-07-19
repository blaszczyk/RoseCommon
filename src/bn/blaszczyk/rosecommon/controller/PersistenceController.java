package bn.blaszczyk.rosecommon.controller;

import java.util.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.*;

import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.rosecommon.RoseException;
import bn.blaszczyk.rosecommon.tools.CommonPreference;
import bn.blaszczyk.rosecommon.tools.EntityUtils;
import bn.blaszczyk.rosecommon.tools.TypeManager;

import static bn.blaszczyk.rosecommon.tools.Preferences.*;
import static bn.blaszczyk.rosecommon.tools.CommonPreference.*;

public class PersistenceController implements ModelController {
	
	private static final Logger LOGGER = LogManager.getLogger(PersistenceController.class);
	private static final Calendar calendar = Calendar.getInstance();

	private static final String KEY_URL = "javax.persistence.jdbc.url";
	private static final String KEY_USER = "javax.persistence.jdbc.user";
	private static final String KEY_PW = "javax.persistence.jdbc.password";

	private static final String TIMESTAMP = "timestamp";
	
	private final EntityManager entityManager;
	
	private final Thread checkDbConnectionThread;

	public PersistenceController() throws RoseException
	{
		
		final Map<String, String> properties = new HashMap<>();
		if(!isDefault(DB_HOST) && !isDefault(DB_PORT) && !isDefault(DB_NAME))
			properties.put(KEY_URL, String.format("jdbc:mysql://%s:%s/%s",getStringValue(DB_HOST),getStringValue(DB_PORT), getStringValue(DB_NAME)));
		if(!isDefault(DB_USER))
			properties.put(KEY_USER, getStringValue(DB_USER));
		if(!isDefault(DB_PASSWORD))
			properties.put(KEY_PW, getStringValue(DB_PASSWORD));
		
		try
		{
			final EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("rosePersistenceUnit", properties);
			entityManager = entityManagerFactory.createEntityManager();
			checkDbConnectionThread = new Thread(() -> checkDbConnection(),"check-db-connection");
			checkDbConnectionThread.start();
		}
		catch(Exception e)
		{
			throw RoseException.wrap(e, "Error initializing PersistenceController");
		}

	}
	
	private void checkDbConnection()
	{
		final int pingDbInterval = getIntegerValue(CommonPreference.DB_PING_INTERVAL);
		while(true)
		{
			try
			{
				Thread.sleep(pingDbInterval);
				synchronized(entityManager)
				{
					final Query query = entityManager.createNativeQuery("SELECT 1");
					try
					{
						LOGGER.debug("pinging database");
						query.getSingleResult();
					}
					catch(Exception e)
					{
						LOGGER.error("database ping unsucessful",e);
					}
				}
			}
			catch (InterruptedException e)
			{
				LOGGER.warn("terminating check DB connection thread", e);
				break;
			}
		}
	}

	@Override
	public <T extends Readable> List<T> getEntities(final Class<T> type) throws RoseException
	{
		try
		{
			LOGGER.debug("start load" + " " + type.getSimpleName());
			synchronized (entityManager)
			{
				final Class<? extends T> implType = TypeManager.getImplClass(type);
				final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
				final CriteriaQuery<T> query = cb.createQuery(type);
				final Root<? extends T> root = query.from(implType);
				query.select(root);

				int fetchTimeSpan = getIntegerValue(FETCH_TIMESPAN);
				if(fetchTimeSpan != Integer.MAX_VALUE)
				{
					calendar.setTime(new Date());
					calendar.add(Calendar.DATE, - fetchTimeSpan);
					cb.greaterThanOrEqualTo(root.get(TIMESTAMP), calendar.getTime());
					LOGGER.debug("fetch entity age restriction: " + fetchTimeSpan + " days");
				}
				final List<T> list = entityManager.createQuery(query).getResultList();
				
				LOGGER.debug("end load entities: " + type.getName() + " count=" + list.size());
				return list;
				
			}
		}
		catch(Exception e)
		{
			throw new RoseException("error loading entities: " + type.getName(), e);
		}
	}
	
	@Override
	public <T extends Readable> List<Integer> getIds(final Class<T> type) throws RoseException
	{
		final String message = "fetching all ids of" + type.getSimpleName();
		try
		{
			synchronized (entityManager)
			{
				LOGGER.debug("start " + message);
				final Class<? extends T> implType = TypeManager.getImplClass(type);
				final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
				final CriteriaQuery<Integer> query = cb.createQuery(Integer.class);
				final Root<? extends T> root = query.from(implType);
				query.select(root.get("id"));
				final int fetchTimeSpan = getIntegerValue(FETCH_TIMESPAN);
				if(fetchTimeSpan != Integer.MAX_VALUE)
				{
					calendar.setTime(new Date());
					calendar.add(Calendar.DATE, - fetchTimeSpan);
					cb.greaterThanOrEqualTo(root.get(TIMESTAMP), calendar.getTime());
				}
				
				final List<Integer> list = entityManager.createQuery(query).getResultList();
				LOGGER.debug("end " + message + " count=" + list.size());
				return list;
			}
		}
		catch(Exception e)
		{
			throw new RoseException("error " + message, e);
		}
	}

	@Override
	public <T extends Readable> int getEntityCount(final Class<T> type) throws RoseException
	{
		final String message = "fetching count for " + type.getSimpleName();
		try
		{
			synchronized (entityManager)
			{
				LOGGER.debug("start " + message);
				final Class<? extends T> implType = TypeManager.getImplClass(type);
				final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
				final CriteriaQuery<Long> query = cb.createQuery(Long.class);
				final Root<? extends T> root = query.from(implType);
				query.select(cb.count(root));

				final Long count = entityManager.createQuery(query).getSingleResult();
				LOGGER.debug("end " + message + " count= " + count);
				return count.intValue();
			}
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e, "error getting count for " + type.getSimpleName());
		}
	}

	@Override
	public <T extends Readable> T getEntityById(final Class<T> type, final int id) throws RoseException
	{
		final String message = "fetching " + type.getSimpleName() + " id=" + id;
		try
		{
			synchronized (entityManager)
			{
				LOGGER.debug("start " + message);
				final Class<? extends T> implType = TypeManager.getImplClass(type);
				final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
				final CriteriaQuery<T> query = cb.createQuery(type);
				final Root<? extends T> root = query.from(implType);
				query.where(cb.equal(root.get("id"), id));
				LOGGER.debug("end " + message);
				final T result = entityManager.createQuery(query).getSingleResult();
				if(result == null)
					throw new RoseException(type.getSimpleName() + " with id=" + id + " not found.");
				return result;
			}
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e, "error getting " + type.getSimpleName() + " with id=" + id);
		}
	}
	
	@Override
	public <T extends Readable> List<T> getEntitiesByIds(final Class<T> type, final List<Integer> ids)
			throws RoseException
	{
		if(ids.isEmpty())
			return Collections.emptyList();
		final String message = "fetching " + type.getSimpleName() + " ids=" + ids;
		try
		{
			synchronized (entityManager)
			{
				LOGGER.debug("start " + message);
				final Class<? extends T> implType = TypeManager.getImplClass(type);
				final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
				final CriteriaQuery<T> query = cb.createQuery(type);
				final Root<? extends T> root = query.from(implType);
				query.where(root.get("id").in(ids));
				final List<T> entities = entityManager.createQuery(query).getResultList();
				LOGGER.debug("start " + message);
				return entities;
			}
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e, "error getting " + type.getSimpleName() + " with ids=" + ids);
		}
	}

	@Override
	public void update(Writable... entities) throws RoseException
	{
		try
		{
			synchronized (entityManager)
			{
				LOGGER.debug("start update");
				final EntityTransaction transaction = entityManager.getTransaction();
				transaction.begin();
				for(Writable entity : entities)
				{
					if(entity == null)
						continue;
					if(entity.getId() < 0)
					{
						throw new RoseException("Impossible Id: " + entity.getId());
					}
					else
					{
						LOGGER.debug("updating entity:\r\n" + EntityUtils.toStringFull(entity));
						entityManager.merge(entity);
					}
				}
				transaction.commit();
				LOGGER.debug("end update");
			}
		}
		catch(Exception e)
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
		final Set<Writable> changedEntities = new LinkedHashSet<>();
		synchronized (entityManager)
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
				entityManager.remove(entity);
				LOGGER.debug("end deleting entity: " + EntityUtils.toStringSimple(entity));
			}
			catch(Exception e)
			{
				throw new RoseException("error deleting " + entity, e);
			}			
		}
	}

	@Override
	public <T extends Readable> T createNew(final Class<T> type) throws RoseException
	{
		final T entity = TypeManager.newInstance(type);
		synchronized (entityManager)
		{
			LOGGER.debug("start creating " + type.getSimpleName());
			final EntityTransaction transaction = entityManager.getTransaction();
			transaction.begin();
			entityManager.persist(entity);
			transaction.commit();
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
	public void close() throws RoseException
	{
		try
		{
			entityManager.close();
			checkDbConnectionThread.interrupt();
		}
		catch(Exception e)
		{
			throw RoseException.wrap(e, "Error closing EntityManager");
		}
	}
	
	public List<?> listQuery( final String query ) throws RoseException
	{
		try
		{
			synchronized (entityManager)
			{
				LOGGER.debug("start query \"" + query + "\"");
				final List<?> list = entityManager.createNamedQuery(query).getResultList();
				if(list == null)
					return Collections.emptyList();
				return list;
			}
		}
		catch(Exception e)
		{
			throw new RoseException("Unable to execute query '" + query + "'", e);
		}
	}
	
}
