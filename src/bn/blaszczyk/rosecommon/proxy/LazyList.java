package bn.blaszczyk.rosecommon.proxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.rosecommon.RoseException;

public class LazyList implements List<Writable> 
{
	private final Logger LOGGER = Logger.getLogger(LazyList.class);
	
	private List<Writable> list;
	
	private final Class<? extends Readable> type;
	private final List<Integer> ids;
	private final EntityAccess access;
	
	private final boolean providingLazyIterator;
	
	private boolean allFetched = false;

	public LazyList(final Class<? extends Readable> type, final List<Integer> ids, final EntityAccess access, final boolean providingLazyIterator)
	{
		list = new ArrayList<>(Collections.nCopies(ids.size(), null));
		this.type = type;
		this.ids = new ArrayList<>(ids);
		this.access = access;
		this.providingLazyIterator = providingLazyIterator;
	}

	public LazyList(final Class<? extends Readable> type, final List<Integer> ids, final EntityAccess access)
	{
		this(type, ids, access, false);
	}

	private void fetchAll()
	{
		if(allFetched)
			return;
		try
		{
			list = access.getMany(type, ids)
						.stream()
						.map(Writable.class::cast)
						.collect(Collectors.toList());
			allFetched = true;
		}
		catch (RoseException e)
		{
			LOGGER.error("Unable to fetch " + type + " ids=" + ids, e);
		}
	}
	
	private void fetch(int index)
	{
		if(allFetched)
			return;
		final Integer id = ids.get(index);
		if(id == null)
			return;
		ids.set(index, null);
		try
		{
			final Writable entity = access.getOne(type, id);
			list.set(index, entity);
		}
		catch (RoseException e) 
		{
			LOGGER.error("Unable to fetch " + type.getSimpleName() + " with id=" + id, e);
		}
	}

	@Override
	public Stream<Writable> stream()
	{
		fetchAll();
		return list.stream();
	}

	@Override
	public Stream<Writable> parallelStream()
	{
		fetchAll();
		return list.parallelStream();
	}

	@Override
	public boolean contains(Object o)
	{
		if(o instanceof Writable)
		{
			int id = ((Writable)o).getId();
			return ids.contains(id);
		}
		return false;
	}
	
	@Override
	public int indexOf(Object o)
	{
		if(o instanceof Writable)
		{
			int id = ((Writable)o).getId();
			return ids.indexOf(id);
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o)
	{
		if(o instanceof Writable)
		{
			int id = ((Writable)o).getId();
			return ids.lastIndexOf(id);
		}
		return -1;
	}

	@Override
	public Object[] toArray()
	{
		fetchAll();
		return list.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a)
	{
		fetchAll();
		return list.toArray(a);
	}

	@Override
	public Writable get(int index)
	{
		fetch(index);
		return list.get(index);
	}

	@Override
	public ListIterator<Writable> listIterator(int index)
	{
		fetchAll();
		return list.listIterator(index);
	}

	@Override
	public ListIterator<Writable> listIterator()
	{
		fetchAll();
		return list.listIterator();
	}

	@Override
	public Iterator<Writable> iterator()
	{
		if(allFetched || !providingLazyIterator)
			return list.iterator();
		return new LazyIterator();
	}

	@Override
	public List<Writable> subList(int fromIndex, int toIndex)
	{
		return new LazyList(type, ids.subList(fromIndex, toIndex), access);
	}

	@Override
	public Spliterator<Writable> spliterator()
	{
		fetchAll();
		return list.spliterator();
	}

	@Override
	public boolean containsAll(Collection<?> c)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString()
	{
		return "LazyList of " + list.toString();
	}

	@Override
	public int size()
	{
		return list.size();
	}

	@Override
	public boolean isEmpty()
	{
		return list.isEmpty();
	}

	@Override
	public boolean add(Writable e)
	{
		ids.add(null);
		return list.add(e);
	}

	@Override
	public boolean remove(Object o)
	{
		final int index = list.indexOf(o);
		if(index < 0)
			return false;
		ids.remove(index);
		return list.remove(o);
	}

	@Override
	public boolean addAll(Collection<? extends Writable> c)
	{
		ids.addAll(Collections.nCopies(c.size(), null));
		return list.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends Writable> c)
	{
		ids.addAll(Collections.nCopies(c.size(), null));
		return list.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear()
	{
		allFetched = true;
		ids.clear();
		list.clear();
	}

	@Override
	public Writable set(int index, Writable element)
	{
		ids.set(index, null);
		return list.set(index, element);
	}

	@Override
	public void add(int index, Writable element)
	{
		ids.add(index, null);
		list.add(index,element);
	}

	@Override
	public Writable remove(int index)
	{
		ids.remove(index);
		return list.remove(index);
	}
	
	private class LazyIterator implements Iterator<Writable>
	{
		int cursor = 0;

		@Override
		public boolean hasNext()
		{
			return cursor < list.size();
		}

		@Override
		public Writable next()
		{
			if(cursor >= list.size() )
				throw new NoSuchElementException();
			fetch(cursor);
			final Writable entity = list.get(cursor);
			cursor++;
			return entity;
		}
	}
}
