package de.slackspace.openkeepass.domain.filter;

public interface Filter<T> {

    boolean matches(T item);
}
