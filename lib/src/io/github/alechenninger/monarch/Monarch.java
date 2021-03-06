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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Monarch {
  /**
   * Generates new data for all known sources in the hierarchy based on the hierarchy, the data
   * changes you want applied, the existing state of the data, and a "target" source which you want
   * to change alongside all of its children.
   *
   * @param hierarchy A tree-structure describing which sources inherit from which parent sources.
   * @param changes The "end-state" changes to be applied assuming you could update the entire tree.
   *                Even if you are not targeting the root of the tree (which would generate an
   *                entirely new tree), the {@code target} and its children will be updated
   *                such that each source's inherited values are what you want your end state to be,
   *                as described by the changes.
   * @param target A source in the hierarchy that represents a level that we can start to
   *                    change values. Sources above this source will be untouched. This source and
   *                    any below it will be updated to achieve the desired {@code changes}.
   * @param data A map of sources to key:value pairs representing the existing state of the data.
   * @param mergeKeys A list of keys whose flattened value is a <em>merge</em> of all of the values
   *                  in the hierarchy for that key, provided the values are all either
   *                  {@link Collection collections} or {@link Map maps}. Keys not in the list
   *                  use values from only the nearest in a sources ancestry.
   * @return A map of sources to key:value pairs representing the new state of the data with changes
   *         applied to the given {@code target} and its children.
   */
  public Map<String, Map<String, Object>> generateSources(Hierarchy hierarchy,
      Iterable<Change> changes, String target, Map<String, Map<String, Object>> data,
      Set<String> mergeKeys) {
    List<String> descendants = hierarchy.descendantsOf(target)
        .orElseThrow(() -> new IllegalArgumentException("Could not find target in " +
            "hierarchy. Target: " + target + ". Hierarchy: \n" + hierarchy));
    Map<String, Map<String, Object>> result = copyMapAndValues(data);

    // From top-most to inner-most, generate results, taking into account the results from ancestors
    // as we go along.
    for (String descendant : descendants) {
      result.put(descendant, generateSingleSource(hierarchy, changes, descendant, result,
          mergeKeys));
    }

    return result;
  }

  public List<Change> findChangesForSource(String source, Iterable<Change> changes) {
    return StreamSupport.stream(changes.spliterator(), false)
        .filter(c -> Objects.equals(c.source(), source))
        .collect(Collectors.toList());
  }

  /**
   * Generates new data for the given source only, taking into account the desired changes, the
   * existing hierarchy, and the existing data in the hierarchy.
   */
  private Map<String, Object> generateSingleSource(Hierarchy hierarchy, Iterable<Change> changes,
      String target, Map<String, Map<String, Object>> data, Set<String> mergeKeys) {
    List<String> ancestors = hierarchy.ancestorsOf(target)
        .orElseThrow(() -> new IllegalArgumentException(
            "Could not find target in hierarchy. Target: " + target + ". " +
                "Hierarchy: \n" + hierarchy));

    DataLookup sourceLookup = new DataLookupFromMap(data, target, hierarchy, mergeKeys);

    Map<String, Object> sourceData = data.get(target);
    Map<String, Object> resultSourceData = sourceData == null
        ? new HashMap<>()
        : new HashMap<>(sourceData);

    for (String ancestor : new ListReversed<>(ancestors)) {
      List<Change> changesForSource = findChangesForSource(ancestor, changes);

      for (Change change : changesForSource) {

        for (Map.Entry<String, Object> setEntry : change.set().entrySet()) {
          String setKey = setEntry.getKey();
          Object setValue = setEntry.getValue();

          if (!Objects.equals(change.source(), target)) {
            if (sourceLookup.isValueInherited(setKey, setValue)) {
              if (resultSourceData.containsKey(setKey)) {
                if (mergeKeys.contains(setKey)) {
                  Merger merger = Merger.startingWith(resultSourceData.get(setKey));
                  merger.unmerge(setValue);
                  resultSourceData.put(setKey, merger.getMerged());
                } else {
                  resultSourceData.remove(setKey);
                }
              }
              continue;
            }
          }

          Object newValue;
          Object currentValue = resultSourceData.get(setKey);

          if (mergeKeys.contains(setKey) && currentValue != null) {
            Merger merger = Merger.startingWith(currentValue);
            merger.merge(setValue);
            newValue = merger.getMerged();
          } else {
            newValue = setValue;
          }

          resultSourceData.put(setKey, newValue);
        }

        // TODO: Support removing nested keys (keys in a hash)
        for (String key : change.remove()) {
          resultSourceData.remove(key);
        }
      }
    }

    return resultSourceData;
  }

  private static Map<String, Map<String, Object>> copyMapAndValues(
      Map<String, Map<String, Object>> data) {
    Map<String, Map<String, Object>> copy = new HashMap<>();

    for (Map.Entry<String, Map<String, Object>> entry : data.entrySet()) {
      String key = entry.getKey();
      Map value = entry.getValue();
      copy.put(key, value == null ? new HashMap<>() : new HashMap<>(value));
    }

    return copy;
  }
}
