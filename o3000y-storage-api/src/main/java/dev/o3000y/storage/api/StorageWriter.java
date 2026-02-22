package dev.o3000y.storage.api;

import dev.o3000y.model.Span;
import java.util.List;

public interface StorageWriter {

  void write(List<Span> spans);
}
