package bn.blaszczyk.rosecommon.controller;

import java.util.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.*;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.EntityField;
import bn.blaszczyk.rose.model.EntityModel;
import bn.blaszczyk.rose.model.Field;
import bn.blaszczyk.rose.model.PrimitiveField;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.rosecommon.tools.CommonPreference;
import bn.blaszczyk.rosecommon.tools.EntityUtils;
import bn.blaszczyk.rosecommon.tools.TypeManager;

import static bn.blaszczyk.rosecommon.tools.Preferences.*;
import static bn.blaszczyk.rosecommon.tools.CommonPreference.*;

final class PersistenceController implements ModelController
{
	public static final String KEY_URL = "javax.persistence.jdbc.url";
	public static final String KEY_USER = "javax.persistence.jdbc.user";
	public static final String KEY_PW = "javax.persistence.jdbc.password";
	public static final String KEY_PERSISTENCE_UNIT = "bn.blaszczyk.rosecommon.persistence-unit";
	
	private static final Logger LOGGER = LogManager.getLogger(PersistenceController.class);
	private static final Calendar calendar = Calendar.getInstance();

	private static final String TIMESTAMP = "timestamp";
	private static final String DEFAULT_PERSISTENCE_UNIT = "rosePersistenceUnit";
	
	private final EntityManager entityManager;
	
	private final Thread checkDbConnectionThread;

	PersistenceController(final Map<String, String> properties) throws RoseException
	{
		try
		{
			final String persistenceUnit = properties.containsKey(KEY_PERSISTENCE_UNIT) ? properties.get(KEY_PERSISTENCE_UNIT) : DEFAULT_PERSISTENCE_UNIT;
			final EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnit, properties);
			entityManager = entityManagerFactory.createEntityManager();
			checkDbConnectionThread = new Thread(() -> checkDbConnection(),"check-db-connection");
			checkDbConnectionThread.start();
		}
		catch(Exception e)
		{
			throw RoseException.wrap(e, "error initializing PersistenceController");
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
				LOGGER.info("terminating check DB connection thread");
				break;
			}
		}
	}

	@Override
	public <T extends Readable> List<T> getEntities(final Class<T> type) throws RoseException
	{
		return getEntities(type, Collections.emptyMap());
	}
	
	@Override
	public <T extends Readable> List<T> getEntities(final Class<T> type, final Map<String, String> query) throws RoseException
	{
		try
		{
			LOGGER.debug("start getting " + type.getSimpleName());
			synchronized (entityManager)
			{
				final TypedQuery<T> typedQuery = createQuery(type, query);

				if(query.containsKey("firstResult"))
				{
					final int firstResult = Integer.parseInt(query.get("firstResult"));
					typedQuery.setFirstResult(firstResult);
				}

				if(query.containsKey("maxResults"))
				{
					final int maxResults = Integer.parseInt(query.get("maxResults"));
					typedQuery.setMaxResults(maxResults);
				}
				
				final List<T> list = typedQuery.getResultList();
				
				LOGGER.debug("end getting " + type.getSimpleName() + " count=" + list.size());
				return list;
			}
		}
		catch(Exception e)
		{
			throw new RoseException("error getting " + type.getSimpleName(), e);
		}
	}

	private <T extends Readable> TypedQuery<T> createQuery(final Class<T> type, final Map<String,String> queryParameters)
	{
		final Class<? extends T> implType = TypeManager.getImplClass(type);

		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<T> query = cb.createQuery(type);
		final Root<? extends T> root = query.from(implType);
		query.select(root).distinct(true);

		transformQuery(queryParameters, type, query, root);

		return entityManager.createQuery(query);
	}
	
	@Override
	public <T extends Readable> List<Integer> getIds(final Class<T> type) throws RoseException
	{
		final String message = "getting all ids of" + type.getSimpleName();
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
	public <T extends Readable> int getEntityCount(final Class<T> type, final Map<String,String> query) throws RoseException
	{
		final String message = "fetching count for " + type.getSimpleName();
		try
		{
			synchronized (entityManager)
			{
				LOGGER.debug("start " + message);
				final Class<? extends T> implType = TypeManager.getImplClass(type);
				final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
				final CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
				final Root<? extends T> root = countQuery.from(implType);
				countQuery.select(cb.count(root));
				
				transformQuery(query, type, countQuery, root);

				final Long count = entityManager.createQuery(countQuery).getSingleResult();
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
				query.select(root).where(cb.equal(root.get("id"), id));
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
				query.select(root).where(root.get("id").in(ids));
				final List<T> entities = entityManager.createQuery(query).getResultList();
				LOGGER.debug("end " + message);
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
						throw new RoseException("illegal id: " + entity.getId());
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
	public <T extends Writable> T createNew(final Class<T> type) throws RoseException
	{
		final T entity = TypeManager.newInstance(type);
		return createNew(entity);
	}
	
	@Override
	public <T extends Writable> T createNew(final T entity) throws RoseException
	{
		synchronized (entityManager)
		{
			LOGGER.debug("start creating " + TypeManager.getClass(entity).getSimpleName());
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
			throw RoseException.wrap(e, "error closing EntityManager");
		}
	}

	private <T extends Readable> void transformQuery(final Map<String, String> queryParameters,
			final Class<T> type, final CriteriaQuery<?> query, final Root<? extends T> root)
	{
		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final EntityModel entityModel = TypeManager.getEntityModel(type);
		final List<Predicate> predicates = new ArrayList<>();

		for(final Field field : entityModel.getFields())
		{
			final String name = field.getName();
			if(queryParameters.containsKey(name))
				predicates.add(cb.equal(root.get(name), EntityUtils.toEntityValue(field, queryParameters.get(name))));
			else if(queryParameters.containsKey(name+"_like") &&
					field instanceof PrimitiveField &&
					((PrimitiveField)field).getType().getJavaType().equals(String.class))
				predicates.add(cb.like(root.get(name), "%"+queryParameters.get(name+"_like")+"%"));
		}
		
		for(final EntityField field : entityModel.getEntityFields())
		{
			final String name = field.getName();
			if(queryParameters.containsKey(name))
			{
				final int id = Integer.parseInt(queryParameters.get(name));
				predicates.add(cb.equal(root.join(name).get("id"), id));
			}
		}

		final boolean isOr = queryParameters.containsKey("useOr");
		if(!predicates.isEmpty())
		{
			final Predicate[] array = predicates.toArray(new Predicate[predicates.size()]);
			if(isOr)
				query.where(cb.or(array));
			else
				query.where(cb.and(array));
		}

		int fetchTimeSpan = getIntegerValue(FETCH_TIMESPAN);
		if(fetchTimeSpan != Integer.MAX_VALUE)
		{
			calendar.setTime(new Date());
			calendar.add(Calendar.DATE, - fetchTimeSpan);
			query.where(cb.greaterThanOrEqualTo(root.get(TIMESTAMP), calendar.getTime()));
			LOGGER.debug("fetch entity age restriction: " + fetchTimeSpan + " days");
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
			throw new RoseException("unable to execute query '" + query + "'", e);
		}
	}
	
}
