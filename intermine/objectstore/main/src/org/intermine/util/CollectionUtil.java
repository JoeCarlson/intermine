package org.intermine.util;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Utilities for Collections.
 *
 * @author Kim Rutherford
 * @author Matthew Wakeling
 */

public class CollectionUtil
{
    /**
     * Return a copy of the given map with the object inserted at the given index.
     *
     * @param map the LinkedHashMap to copy
     * @param prevKey the newKey,newValue pair are added after this key.  If null the
     * newKey,newValue pair is added first
     * @param newKey the new key
     * @param newValue the new value
     * @return the copied LinkedHashMap with newKey and newValue added
     */
    public static LinkedHashMap linkedHashMapAdd(LinkedHashMap map, Object prevKey,
                                                 Object newKey, Object newValue) {

        if (prevKey == null) {
            LinkedHashMap newMap = new LinkedHashMap();

            newMap.put(newKey, newValue);

            newMap.putAll(map);

            return newMap;
        }

        if (!map.containsKey(prevKey)) {
            throw new IllegalArgumentException("LinkedHashMap does not contain: " + prevKey);
        }

        LinkedHashMap newMap = new LinkedHashMap();

        Iterator iter = map.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry mapEntry = (Map.Entry) iter.next();

            Object key = mapEntry.getKey();
            Object value = mapEntry.getValue();

            newMap.put(key, value);

            if (key.equals(prevKey)) {
                newMap.put(newKey, newValue);
            }
        }

        return newMap;
    }

    /**
     * Sorts objects in the given collection into different types by Class.
     * @param <E> the element type
     * @param objects a Collection of objects
     * @param inherit if true, objects are put into all their class's superclasses as well, except
     * Object - the original Collection can be used in that case
     * @return a Map from Class to List of objects in that class
     */
    public static <E> Map<Class, List<E>> groupByClass(Collection<E> objects, boolean inherit) {
        Map<Class, List<E>> retval = new HashMap<Class, List<E>>();
        for (E o : objects) {
            Class c = o.getClass();
            if (inherit) {
                Set<Class> done = new HashSet<Class>();
                done.add(Object.class);
                Stack<Class> todo = new Stack<Class>();
                todo.push(c);
                while (!todo.empty()) {
                    c = todo.pop();
                    if ((c != null) && !done.contains(c)) {
                        done.add(c);
                        List<E> l = retval.get(c);
                        if (l == null) {
                            l = new ArrayList<E>();
                            retval.put(c, l);
                        }
                        l.add(o);
                        todo.push(c.getSuperclass());
                        Class classes[] = c.getInterfaces();
                        for (int i = 0; i < classes.length; i++) {
                            todo.push(classes[i]);
                        }
                    }
                }
            } else {
                List<E> l = retval.get(c);
                if (l == null) {
                    l = new ArrayList<E>();
                    retval.put(c, l);
                }
                l.add(o);
            }
        }
        return retval;
    }

    /**
     * Returns a collection containing all possible combinations of the input values.
     * @param <E> the element type
     * @param values a List of collections of values
     * @return a collection of Lists, all the same size as the input List, containing all possible
     * combinations of the values in the collections, in their respective indexes in the List.
     */
    public static <E> Collection<List<E>> fanOutCombinations(List<Collection<E>> values) {
        Collection<List<E>> retval = new ArrayList();
        fanOutCombinations(values, retval, Collections.EMPTY_LIST, 0);
        return retval;
    }

    private static <E> void fanOutCombinations(List<Collection<E>> values,
            Collection<List<E>> retval, List<E> soFar, int index) {
        if (index == values.size() - 1) {
            for (E value : values.get(index)) {
                List<E> solution = new ArrayList(soFar);
                solution.add(value);
                retval.add(solution);
            }
        } else {
            for (E value : values.get(index)) {
                List<E> solution = new ArrayList(soFar);
                solution.add(value);
                fanOutCombinations(values, retval, solution, index + 1);
            }
        }
    }
}
