package com.tools.fsserver.storage;

import com.tools.fsserver.exception.FileNameNotPresentOnServerException;
import com.tools.fsserver.exception.FileNamePresentOnServerException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;

public class FileSystemStorageServiceTest {

  @Test
  public void testStoreFile() throws IOException {
    try (MockedStatic<Files> files = Mockito.mockStatic(Files.class);
        MockedStatic<Paths> paths = Mockito.mockStatic(Paths.class)) {
      files.when(() -> Files.deleteIfExists(any())).thenReturn(true);
      String mockPathAsString = "mockPathAsString";
      Path mockPath = mock(Path.class);
      paths.when(() -> Paths.get(eq(mockPathAsString))).thenReturn(mockPath);
      paths.when(() -> Paths.get(any(), any())).thenReturn(mockPath);
      files.when(() -> Files.notExists(eq(mockPath))).thenReturn(false);
      files.when(() -> Files.copy(any(Path.class), any(Path.class))).thenReturn(mockPath);
      FileSystemStorageService storage = new FileSystemStorageService(mockPathAsString);
      try {
        storage.storeFile("fileName", mockPath);
      } catch (FileNamePresentOnServerException e) {
        fail("shouldn't have thrown FileNamePresentOnServerException");
      }
      files.verify(() -> Files.copy(any(Path.class), any(Path.class)));
    }
  }

  @Test
  public void testDeletingFile() throws IOException {
    try (MockedStatic<Files> files = Mockito.mockStatic(Files.class);
        MockedStatic<Paths> paths = Mockito.mockStatic(Paths.class)) {
      files.when(() -> Files.deleteIfExists(any())).thenReturn(true);
      String mockPathAsString = "mockPathAsString";
      Path mockPath = mock(Path.class);
      paths.when(() -> Paths.get(eq(mockPathAsString))).thenReturn(mockPath);
      files.when(() -> Files.notExists(eq(mockPath))).thenReturn(false);
      FileSystemStorageService storage = new FileSystemStorageService(mockPathAsString);
      try {
        storage.deleteFile("fileToDelete");
      } catch (FileNameNotPresentOnServerException e) {
        fail("shouldn't have thrown FileNameNotPresentOnServerException");
      }
      files.verify(() -> Files.deleteIfExists(any()), atMostOnce());
    }
  }
}
