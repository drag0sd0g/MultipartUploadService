package com.tools.fsclient;

import com.google.common.annotations.VisibleForTesting;
import com.tools.fsclient.rest.FSRestClient;
import org.apache.commons.cli.*;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FSCmdLine {

  private static final Logger LOG = LoggerFactory.getLogger(FSCmdLine.class);
  private static final String FSCLIENT_EXECUTABLE = "file-storage-client";
  private static final String FSCLIENT_PROPERTIES_FILE = "application.properties";
  private static final String FSCLIENT_HELP_HEADER =
      "To the usage command above, this CLI needs exactly one of the options:";
  private static final String FSCLIENT_HELP_FOOTER =
      "Provide an option above in either the short '-' or long'--' version";
  private static final String OPTION_LIST_FILES = "list-files";
  private static final String OPTION_UPLOAD_FILE = "upload-file";
  private static final String OPTION_DELETE_FILE = "delete-file";
  private static final String FSSERVER_ROOT_URL_PROP = "fsserver.api.rootUrl";
  private static final String FSSERVER_API_VERSION_PROP = "fsserver.api.version";
  private static final String FSSERVER_API_FILES = "fsserver.api.filesApi";
  private static final String FSSERVER_API_STATS = "fsserver.api.statsApi";
  private static final HelpFormatter HELP_FORMATTER = new HelpFormatter();
  private static final CommandLineParser CMD_LINE_PARSER = new DefaultParser();

  private FSRestClient fsRestClient;

  public static void main(String[] args) {
    FSCmdLine fsCmdLine = new FSCmdLine();
    fsCmdLine.processInputAndRun(args);
  }

  public FSCmdLine() {
    this(new Configurations());
  }

  @VisibleForTesting
  FSCmdLine(FSRestClient fsRestClient) {
    this.fsRestClient = fsRestClient;
  }

  @VisibleForTesting
  FSCmdLine(Configurations configs) {
    try {
      String rootUrlPassedSysProp = System.getProperty(FSSERVER_ROOT_URL_PROP);
      PropertiesConfiguration config = configs.properties(new File(FSCLIENT_PROPERTIES_FILE));
      // allow overriding of the server's location if we pass one system properties
      String serverApiRootUrl =
          StringUtils.isEmpty(rootUrlPassedSysProp)
              ? config.getString(FSSERVER_ROOT_URL_PROP)
              : rootUrlPassedSysProp;
      LOG.info("Will be contacting FSServer at {}", serverApiRootUrl);
      String serverApiVersion = config.getString(FSSERVER_API_VERSION_PROP);
      String serverFilesApi = config.getString(FSSERVER_API_FILES);
      String serverStatsApi = config.getString(FSSERVER_API_STATS);
      this.fsRestClient =
          new FSRestClient(
              String.join("/", serverApiRootUrl, serverApiVersion, serverFilesApi),
              String.join("/", serverApiRootUrl, serverApiVersion, serverStatsApi));
    } catch (ConfigurationException ex) {
      LOG.error("Startup failure - unable to process configuration", ex);
    }
  }

  /**
   * Main CLI processing logic: args are being parsed, based on which an HTTP call to the server is
   * made for upload/deletion/listing. If any errors occur, the CLI usage guide is logged to console
   * before exiting
   *
   * @param args - cmd line args
   */
  @VisibleForTesting
  void processInputAndRun(String[] args) {
    Options options = buildOptions();
    CommandLine parsedCmdLine;
    try { // parse(...) below will fail if unknown options/args are passed
      parsedCmdLine = CMD_LINE_PARSER.parse(options, args);
    } catch (ParseException e) {
      LOG.error("Error parsing command line input. Please consult the usage guide and try again");
      HELP_FORMATTER.printHelp(
          FSCLIENT_EXECUTABLE, FSCLIENT_HELP_HEADER, options, FSCLIENT_HELP_FOOTER);
      return;
    }
    // At this point, either no options have been passed or one of the allowed ones
    if (ArrayUtils.isEmpty(parsedCmdLine.getOptions())) {
      handleNoCommandsGiven(options);
    } else if (parsedCmdLine.hasOption(OPTION_LIST_FILES)) {
      handleListFilesCommand();
    } else if (parsedCmdLine.hasOption(OPTION_UPLOAD_FILE)) {
      handleFileUploadCommand(parsedCmdLine, options);
    } else if (parsedCmdLine.hasOption(OPTION_DELETE_FILE)) {
      handleFileDeleteCommand(parsedCmdLine);
    } else {
      LOG.error("Unsupported option specified. Please consult the usage guide and try again");
      HELP_FORMATTER.printHelp(
          FSCLIENT_EXECUTABLE, FSCLIENT_HELP_HEADER, options, FSCLIENT_HELP_FOOTER);
    }
  }

  private void handleNoCommandsGiven(Options options) {
    LOG.error("No options specified. Please consult the usage guide and try again");
    HELP_FORMATTER.printHelp(
        FSCLIENT_EXECUTABLE, FSCLIENT_HELP_HEADER, options, FSCLIENT_HELP_FOOTER);
  }

  private void handleListFilesCommand() {
    LOG.debug("Received command to list all uploaded files");
    this.fsRestClient.listUploadedFiles();
  }

  private void handleFileUploadCommand(CommandLine parsedCmdLine, Options options) {
    LOG.debug("Received command to upload a file");
    String pathToFileToUpload = parsedCmdLine.getOptionValue(OPTION_UPLOAD_FILE);
    if (!checkIfFileToUploadExists(pathToFileToUpload)) {
      LOG.error("File {} doesn't exist. Please select a file which exists", pathToFileToUpload);
      HELP_FORMATTER.printHelp(
          FSCLIENT_EXECUTABLE, FSCLIENT_HELP_HEADER, options, FSCLIENT_HELP_FOOTER);
      return;
    }
    this.fsRestClient.uploadFile(pathToFileToUpload);
  }

  private void handleFileDeleteCommand(CommandLine parsedCmdLine) {
    LOG.debug("Received command to delete a file");
    this.fsRestClient.deleteFile(parsedCmdLine.getOptionValue(OPTION_DELETE_FILE));
  }

  @VisibleForTesting
  boolean checkIfFileToUploadExists(String pathToFileToUpload) {
    Path pathToUploadFile = Paths.get(pathToFileToUpload);
    return Files.exists(pathToUploadFile);
  }

  @VisibleForTesting
  FSRestClient getFsRestClient() {
    return this.fsRestClient;
  }

  private Options buildOptions() {
    String fileUploadSizeLimit = this.fsRestClient.getFileUploadSizeLimit();
    String fileSizeInstructions =
        fileUploadSizeLimit.isEmpty()
            ? "within bounds allowed by the server"
            : "<= " + fileUploadSizeLimit;
    Options options = new Options();
    options.addOption(
        Option.builder()
            .option("l")
            .longOpt(OPTION_LIST_FILES)
            .hasArg(false)
            .desc("List all uploaded files on the server. No extra arguments needed")
            .build());
    options.addOption(
        Option.builder()
            .option("u")
            .longOpt(OPTION_UPLOAD_FILE)
            .hasArg(true)
            .desc(
                "Uploads the file provided as argument. The file must exist locally and must have the size "
                    + fileSizeInstructions
                    + " or else an error will be thrown")
            .build());
    options.addOption(
        Option.builder()
            .option("d")
            .longOpt(OPTION_DELETE_FILE)
            .hasArg(true)
            .desc(
                "Deletes from the server the file provided as argument. The file must exist on the server or else an error will be thrown")
            .build());
    return options;
  }
}
