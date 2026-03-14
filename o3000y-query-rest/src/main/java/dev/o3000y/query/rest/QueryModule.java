package dev.o3000y.query.rest;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.o3000y.loadgen.LoadGenService;
import dev.o3000y.model.PipelineMetrics;
import dev.o3000y.query.engine.DuckDbQueryEngine;
import dev.o3000y.query.engine.QueryConfig;
import dev.o3000y.storage.ducklake.DuckLakeManager;
import jakarta.inject.Singleton;

public final class QueryModule extends AbstractModule {

  private final QueryConfig queryConfig;
  private final int restPort;

  public QueryModule(QueryConfig queryConfig, int restPort) {
    this.queryConfig = queryConfig;
    this.restPort = restPort;
  }

  @Provides
  @Singleton
  QueryConfig provideQueryConfig() {
    return queryConfig;
  }

  @Provides
  @Singleton
  DuckDbQueryEngine provideQueryEngine(DuckLakeManager manager, PipelineMetrics metrics) {
    return new DuckDbQueryEngine(queryConfig, manager.newConnection(), metrics);
  }

  @Provides
  @Singleton
  QueryRestApi provideRestApi(
      DuckDbQueryEngine engine, PipelineMetrics metrics, LoadGenService loadGenService) {
    return new QueryRestApi(engine, metrics, loadGenService, restPort);
  }
}
