package com.tasky.app;

import androidx.hilt.work.HiltWorkerFactory;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class TaskyApp_MembersInjector implements MembersInjector<TaskyApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public TaskyApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<TaskyApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new TaskyApp_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(TaskyApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.tasky.app.TaskyApp.workerFactory")
  public static void injectWorkerFactory(TaskyApp instance, HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
