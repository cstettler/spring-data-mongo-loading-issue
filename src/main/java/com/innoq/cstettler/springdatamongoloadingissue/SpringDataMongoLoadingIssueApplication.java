package com.innoq.cstettler.springdatamongoloadingissue;

import static java.lang.String.valueOf;
import static java.util.stream.IntStream.range;

import com.innoq.cstettler.springdatamongoloadingissue.Entity.Nested.NestedA;
import com.innoq.cstettler.springdatamongoloadingissue.Entity.Nested.NestedB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class SpringDataMongoLoadingIssueApplication {

  @Autowired
  private EntityRepository entityRepository;

  public static void main(String[] args) {
    SpringApplication.run(SpringDataMongoLoadingIssueApplication.class, args);
  }

  @EventListener
  public void onApplicationStarted(ApplicationStartedEvent ignored) {
    range(0, 10)
      .mapToObj(index -> new Entity(valueOf(index), index % 2 == 0 ? new NestedA() : new NestedB()))
      .forEach(entityRepository::save);
  }
}
