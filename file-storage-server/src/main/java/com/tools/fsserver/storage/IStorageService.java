package com.tools.fsserver.storage;

import com.tools.fsserver.exception.FileNameNotPresentOnServerException;
import com.tools.fsserver.exception.FileNamePresentOnServerException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public interface IStorageService {
  Set<String> listUploadedFiles() throws IOException;

  void storeFile(String persistingFileName, Path pathToFileToPersist)
      throws FileNamePresentOnServerException, IOException;

  void deleteFile(String fileNameToDelete) throws FileNameNotPresentOnServerException, IOException;
}
