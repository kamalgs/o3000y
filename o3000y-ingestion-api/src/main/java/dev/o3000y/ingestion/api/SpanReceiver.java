package dev.o3000y.ingestion.api;

import dev.o3000y.model.Span;
import java.util.List;

public interface SpanReceiver {

  void receive(List<Span> spans);
}
