package bn.blaszczyk.rosecommon.proxy;

import java.util.List;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.Readable;

public interface EntityAccess {
	
	public <T extends Readable> T getOne(final Class<T> type, final int id) throws RoseException;
	
	public <T extends Readable> List<T> getMany(final Class<T> type, final List<Integer> ids) throws RoseException;
	
}
