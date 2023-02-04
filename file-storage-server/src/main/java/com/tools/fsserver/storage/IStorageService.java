package com.tools.fsserver.storage;

import com.tools.fsserver.exception.FileNameNotPresentOnServerException;
import com.tools.fsserver.exception.FileNamePresentOnServerException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * Generic storage service interface permitting storage of files, deletion and listing stored files.
 * There can be multiple implementations for this e.g. local file system, S3, NoSql, etc
 */
public interface IStorageService {
  Set<String> listStoredFiles() throws IOException;

  void storeFile(String persistingFileName, Path pathToFileToPersist)
      throws FileNamePresentOnServerException, IOException;

  void deleteFile(String fileNameToDelete) throws FileNameNotPresentOnServerException, IOException;
}
