/*
 * monarch - A tool for managing hierarchical data.
 * Copyright (C) 2015  Alec Henninger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.alechenninger.monarch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Change {
  private final String source;
  private final Map<String, Object> set;
  private final List<String> remove;

  public Change(String source, Map<String, Object> set, List<String> remove) {
    this.source = source;
    this.set = Collections.unmodifiableMap(new HashMap<>(set));
    this.remove = Collections.unmodifiableList(new ArrayList<>(remove));
  }

  public static Change fromMap(Map<String, Object> map) {
    if (map == null) {
      throw new IllegalArgumentException("Cannot create a change from 'null'.");
    }
    Map<String, Object> set = (Map<String, Object>) map.get("set");
    List<String> remove = (List<String>) map.get("remove");
    if (set == null) {
      set = Collections.emptyMap();
    }
    if (remove == null) {
      remove = Collections.emptyList();
    }

    return new Change((String) map.get("source"), set, remove);
  }

  public String source() {
    return source;
  }

  public Map<String, Object> set() {
    return set;
  }

  public List<String> remove() {
    return remove;

  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Change change = (Change) o;
    return Objects.equals(source, change.source) &&
        Objects.equals(set, change.set) &&
        Objects.equals(remove, change.remove);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, set, remove);
  }

  @Override
  public String toString() {
    return "Change{" +
        "source='" + source + '\'' +
        ", set=" + set +
        ", remove=" + remove +
        '}';
  }
}
