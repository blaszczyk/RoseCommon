package bn.blaszczyk.rosecommon.controller;

import java.util.List;

import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.rosecommon.RoseException;

public interface ModelController 
{
	
	public <T extends Readable> List<T> getEntities(final Class<T> type) throws RoseException;
	
	public <T extends Readable> List<Integer> getIds(final Class<T> type) throws RoseException;
	
	public <T extends Readable> int getEntityCount(final Class<T> type) throws RoseException;
	
	public <T extends Readable> T getEntityById(final Class<T> type, int id) throws RoseException;
	
	public <T extends Readable> List<T> getEntitiesByIds(final Class<T> type, final List<Integer> ids) throws RoseException;
	
	public <T extends Readable> T createNew(final Class<T> type) throws RoseException;
	
	public Writable createCopy(final Writable entity) throws RoseException;
	
	public void update(final Writable... entities) throws RoseException;

	public void delete(final Writable entity) throws RoseException;

	public void close();
	
}