package com.tools.fsclient;

import com.tools.fsclient.rest.FSRestClient;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FSCmdLineTest {

  @Test
  public void testProcessingConfigurationSuccessfully() throws ConfigurationException {
    Configurations mockConfigs = mock(Configurations.class);
    PropertiesConfiguration mockPropCfg = mock(PropertiesConfiguration.class);
    when(mockPropCfg.getString("fsserver.api.rootUrl")).thenReturn("http://localhost:8081");
    when(mockPropCfg.getString("fsserver.api.version")).thenReturn("v1");
    when(mockPropCfg.getString("fsserver.api.filesApi")).thenReturn("files");
    when(mockPropCfg.getString("fsserver.api.statsApi")).thenReturn("stats");
    when(mockConfigs.properties(any(File.class))).thenReturn(mockPropCfg);
    FSCmdLine fsCmdLine = new FSCmdLine(mockConfigs);
    assertEquals("http://localhost:8081/v1/files", fsCmdLine.getFsRestClient().getServerFilesApi());
  }

  @Test
  public void testProcessingListFilesCommand() {
    FSRestClient fsRestClient = mock(FSRestClient.class);
    when(fsRestClient.getFileUploadSizeLimit()).thenReturn("");
    FSCmdLine fsCmdLine = new FSCmdLine(fsRestClient);
    String[] argsShort = {"-l"};
    fsCmdLine.processInputAndRun(argsShort);
    verify(fsRestClient).listUploadedFiles();
    verify(fsRestClient, never()).uploadFile(any());
    verify(fsRestClient, never()).deleteFile(any());
    reset(fsRestClient);
    String[] argsLong = {"--list-files"};
    when(fsRestClient.getFileUploadSizeLimit()).thenReturn("");
    fsCmdLine.processInputAndRun(argsLong);
    verify(fsRestClient).listUploadedFiles();
    verify(fsRestClient, never()).uploadFile(any());
    verify(fsRestClient, never()).deleteFile(any());
  }

  @Test
  public void testProcessingDeleteFileCommandWithParam() {
    FSRestClient fsRestClient = mock(FSRestClient.class);
    when(fsRestClient.getFileUploadSizeLimit()).thenReturn("");
    FSCmdLine fsCmdLine = new FSCmdLine(fsRestClient);
    String[] argsShort = {"-d", "fileToDelete.txt"};
    fsCmdLine.processInputAndRun(argsShort);
    verify(fsRestClient, never()).listUploadedFiles();
    verify(fsRestClient, never()).uploadFile(any());
    verify(fsRestClient).deleteFile(any());
    reset(fsRestClient);
    String[] argsLong = {"--delete-file", "fileToDelete.txt"};
    when(fsRestClient.getFileUploadSizeLimit()).thenReturn("");
    fsCmdLine.processInputAndRun(argsLong);
    verify(fsRestClient, never()).listUploadedFiles();
    verify(fsRestClient, never()).uploadFile(any());
    verify(fsRestClient).deleteFile(any());
  }

  @Test
  public void testProcessingDeleteFileCommandWithNoParamExpectNoRestClientInvocation() {
    FSRestClient fsRestClient = mock(FSRestClient.class);
    FSCmdLine fsCmdLine = new FSCmdLine(fsRestClient);
    when(fsRestClient.getFileUploadSizeLimit()).thenReturn("");
    String[] argsShort = {"-d"};
    fsCmdLine.processInputAndRun(argsShort);
    verify(fsRestClient, never()).listUploadedFiles();
    verify(fsRestClient, never()).uploadFile(any());
    verify(fsRestClient, never()).deleteFile(any());
    reset(fsRestClient);
    String[] argsLong = {"--delete-file"};
    when(fsRestClient.getFileUploadSizeLimit()).thenReturn("");
    fsCmdLine.processInputAndRun(argsLong);
    verify(fsRestClient, never()).listUploadedFiles();
    verify(fsRestClient, never()).uploadFile(any());
    verify(fsRestClient, never()).deleteFile(any());
  }

  @Test
  public void testProcessingUploadFileCommandWithNoParamExpectNoRestClientInvocation() {
    FSRestClient fsRestClient = mock(FSRestClient.class);
    when(fsRestClient.getFileUploadSizeLimit()).thenReturn("");
    FSCmdLine fsCmdLine = new FSCmdLine(fsRestClient);
    String[] argsShort = {"-u"};
    fsCmdLine.processInputAndRun(argsShort);
    verify(fsRestClient, never()).listUploadedFiles();
    verify(fsRestClient, never()).uploadFile(any());
    verify(fsRestClient, never()).deleteFile(any());
    reset(fsRestClient);
    String[] argsLong = {"--upload-file"};
    when(fsRestClient.getFileUploadSizeLimit()).thenReturn("");
    fsCmdLine.processInputAndRun(argsLong);
    verify(fsRestClient, never()).listUploadedFiles();
    verify(fsRestClient, never()).uploadFile(any());
    verify(fsRestClient, never()).deleteFile(any());
  }

  @Test
  public void testProcessingUploadFileCommandWithParam() {
    FSRestClient fsRestClient = mock(FSRestClient.class);
    when(fsRestClient.getFileUploadSizeLimit()).thenReturn("");
    FSCmdLine fsCmdLine = spy(new FSCmdLine(fsRestClient));
    doReturn(true).when(fsCmdLine).checkIfFileToUploadExists(any(String.class));
    String[] argsShort = {"-u", "fileToUpload.txt"};
    fsCmdLine.processInputAndRun(argsShort);
    verify(fsRestClient, never()).listUploadedFiles();
    verify(fsRestClient).uploadFile(any());
    verify(fsRestClient, never()).deleteFile(any());
    reset(fsRestClient);
    String[] argsLong = {"--upload-file", "fileToUpload.txt"};
    when(fsRestClient.getFileUploadSizeLimit()).thenReturn("");
    fsCmdLine.processInputAndRun(argsLong);
    verify(fsRestClient, never()).listUploadedFiles();
    verify(fsRestClient).uploadFile(any());
    verify(fsRestClient, never()).deleteFile(any());
  }

  @Test
  public void testProcessingEmptyInputExpectNoInvocationOfRestClient() {
    FSRestClient fsRestClient = mock(FSRestClient.class);
    when(fsRestClient.getFileUploadSizeLimit()).thenReturn("");
    FSCmdLine fsCmdLine = new FSCmdLine(fsRestClient);
    String[] emptyArgs = {};
    fsCmdLine.processInputAndRun(emptyArgs);
    verify(fsRestClient, never()).listUploadedFiles();
    verify(fsRestClient, never()).uploadFile(any());
    verify(fsRestClient, never()).deleteFile(any());
  }

  @Test
  public void testProcessingUnsupportedOptionExpectNoInvocationOfRestClient() {
    FSRestClient fsRestClient = mock(FSRestClient.class);
    FSCmdLine fsCmdLine = new FSCmdLine(fsRestClient);
    when(fsRestClient.getFileUploadSizeLimit()).thenReturn("");
    String[] emptyArgs = {"--unsupported-option"};
    fsCmdLine.processInputAndRun(emptyArgs);
    verify(fsRestClient, never()).listUploadedFiles();
    verify(fsRestClient, never()).uploadFile(any());
    verify(fsRestClient, never()).deleteFile(any());
  }
}
