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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CliInputs implements Inputs {
  // NOTE: Special unicode whitespace used in descriptions to defeat help format whitespace trim.
  private final Option hierarchyOption = Option.builder("h")
      .longOpt("hierarchy")
      .hasArg()
      .argName("path")
      .desc("Path to a yaml file describing the source hierarchy, relative to the data directory "
          + "(see data-dir option). For example: \n"
          + "global.yaml:\n"
          + "  teams/myteam.yaml:\n"
          + "    teams/myteam/dev.yaml\n"
          + "    teams/myteam/stage.yaml\n"
          + "    teams/myteam/prod.yaml\n"
          + "  teams/otherteam.yaml")
      .build();

  private final Option changesOption = Option.builder("c")
      .longOpt("changes")
      .hasArg()
      .argName("path")
      .desc("Path to a yaml file describing the desired end-state changes. For example: \n"
          + "---\n"
          + "  source: teams/myteam.yaml\n"
          + "  set:\n"
          + "    myapp::version: 2\n"
          + "    myapp::favorite_website: http://www.redhat.com\n"
          + "---\n"
          + "  source: teams/myteam/stage.yaml\n"
          + "  set:\n"
          + "    myapp::favorite_website: http://stage.redhat.com")
      .build();

  private final Option targetOption = Option.builder("t")
      .longOpt("target")
      .hasArg()
      .argName("target")
      .desc("A target is the source in the source tree from where you want to change, "
          + "including itself and any sources beneath it in the hierarchy. Redundant keys will be "
          + "removed in sources beneath the target (that is, sources which inherit its values). "
          + "Ex: 'teams/myteam.yaml'")
      .build();

  private final Option dataDirectoryOption = Option.builder("d")
      .longOpt("data-dir")
      .argName("path")
      .hasArg()
      .desc("Path to where existing data sources life. The data for sources describe in the "
          + "hierarchy is looked using the paths in the hierarchy relative to this folder.")
      .build();

  private final Option configPathsOption = Option.builder()
      .longOpt("configs")
      .argName("path1 path2")
      .hasArgs()
      .desc("Space delimited paths to files which configures default values for command line "
          + "options. The default config path of ~/.monarch/config.yaml is always checked.")
      .build();

  private final Option outputDirOption = Option.builder("o")
      .longOpt("output-dir")
      .argName("path")
      .hasArg()
      .desc("Path to directory where result data sources will be written. Data sources will be "
          + "written using relative paths from hierarchy.")
      .build();

  private final Option mergeKeysOption = Option.builder("m")
      .longOpt("merge-keys")
      .argName("k1,k2")
      .hasArg()
      .desc("Comma-delimited list of keys which should be inherited with merge semantics. That is, "
          + "normally the value that is inherited for a given key is only the nearest ancestor's "
          + "value. Keys that are in the merge key list however inherit values from all of their "
          + "ancestor's and merge them together, provided they are like types of either "
          + "collections or maps.")
      .build();

  private final Option helpOption = Option.builder("?")
      .longOpt("help")
      .desc("Show this text.")
      .build();

  private final Options options = new Options()
      .addOption(hierarchyOption)
      .addOption(changesOption)
      .addOption(targetOption)
      .addOption(dataDirectoryOption)
      .addOption(configPathsOption)
      .addOption(outputDirOption)
      .addOption(mergeKeysOption)
      .addOption(helpOption)
      ;

  private final CommandLine cli;
  private final HelpFormatter helpFormatter = new HelpFormatter();

  public static CliInputs parse(String[] args) throws ParseException {
    return parse(args, new DefaultParser());
  }

  public static CliInputs parse(String[] args, CommandLineParser parser) throws ParseException {
    return new CliInputs(args, parser);
  }

  private CliInputs(String args[], CommandLineParser cliParser) throws ParseException {
    cli = cliParser.parse(options, args);
  }

  @Override
  public Optional<String> getHierarchyPathOrYaml() {
    return Optional.ofNullable(cli.getOptionValue(hierarchyOption.getOpt()));
  }

  @Override
  public Optional<String> getChangesPathOrYaml() {
    return Optional.ofNullable(cli.getOptionValue(changesOption.getOpt()));
  }

  @Override
  public Optional<String> getTarget() {
    return Optional.ofNullable(cli.getOptionValue(targetOption.getOpt()));
  }

  @Override
  public Optional<String> getDataDir() {
    return Optional.ofNullable(cli.getOptionValue(dataDirectoryOption.getOpt()));
  }

  @Override
  public List<String> getConfigPaths() {
    String[] maybeConfigPaths = cli.getOptionValues(configPathsOption.getLongOpt());
    return Arrays.asList(
        Optional.ofNullable(maybeConfigPaths).orElse(new String[0]));
  }

  @Override
  public Optional<String> getOutputDir() {
    return Optional.ofNullable(cli.getOptionValue(outputDirOption.getOpt()));
  }

  @Override
  public Optional<String> getMergeKeys() {
    return Optional.ofNullable(cli.getOptionValue(mergeKeysOption.getOpt()));
  }

  public boolean helpRequested() {
    return cli.hasOption(helpOption.getOpt());
  }

  public String helpMessage() {
    StringWriter result = new StringWriter();
    PrintWriter printWriter = new PrintWriter(result);

    helpFormatter.printHelp(printWriter, 80,
        "monarch --hierarchy hierarchy.yaml --changes changes.yaml --target teams/myteam.yaml "
            + "--data-dir ~/hieradata --output-dir ./", null, options, HelpFormatter.DEFAULT_LEFT_PAD,
        HelpFormatter.DEFAULT_DESC_PAD, "https://github.com/alechenninger/monarch");

    return result.toString();
  }
}
