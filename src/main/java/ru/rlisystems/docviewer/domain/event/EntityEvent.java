package ru.rlisystems.docviewer.domain.event;

import lombok.Getter;

public class EntityEvent<T>
{
	@Getter
	private final Class<T> entityClass;

	@Getter
	private final T entity;

	@SuppressWarnings ("unchecked")
	private EntityEvent (T entity)
	{
		this.entity = entity;
		this.entityClass = (Class<T>) entity.getClass();
	}

	private EntityEvent (Class<T> entityClass)
	{
		this.entityClass = entityClass;
		this.entity = null;
	}

	public static <T> EntityEvent<T> create (T entity)
	{
		return new EntityEvent<>(entity);
	}

	public static <T> EntityEvent<T> create (Class<T> entityClass)
	{
		return new EntityEvent<T>(entityClass);
	}
}
