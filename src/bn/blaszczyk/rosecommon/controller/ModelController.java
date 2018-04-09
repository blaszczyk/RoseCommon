package bn.blaszczyk.rosecommon.controller;

import java.util.List;
import java.util.Map;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Writable;

public interface ModelController 
{
	
	public <T extends Readable> List<T> getEntities(final Class<T> type) throws RoseException;
	
	public <T extends Readable> List<T> getEntities(final Class<T> type, final Map<String,String> query) throws RoseException;
	
	public <T extends Readable> List<Integer> getIds(final Class<T> type) throws RoseException;
	
	public <T extends Readable> int getEntityCount(final Class<T> type) throws RoseException;
	
	public <T extends Readable> T getEntityById(final Class<T> type, int id) throws RoseException;
	
	public <T extends Readable> List<T> getEntitiesByIds(final Class<T> type, final List<Integer> ids) throws RoseException;
	
	public <T extends Writable> T createNew(final Class<T> type) throws RoseException;
	
	public <T extends Writable> T createNew(final T entity) throws RoseException;
	
	public Writable createCopy(final Writable entity) throws RoseException;
	
	public void update(final Writable... entities) throws RoseException;

	public void delete(final Writable entity) throws RoseException;

	public void close() throws RoseException;
	
}