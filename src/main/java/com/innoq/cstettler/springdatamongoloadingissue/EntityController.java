package com.innoq.cstettler.springdatamongoloadingissue;

import static java.lang.String.valueOf;
import static java.util.stream.IntStream.range;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EntityController {

  @Autowired
  private EntityRepository entityRepository;

  @Value("${enable-workaround:false}")
  private boolean enableWorkaround;

  @PostMapping("/entities")
  public void initializeEntities(@RequestParam(name = "count", defaultValue = "10") int count) {
    entityRepository.deleteAll();

    range(0, count)
      .mapToObj(index -> new Entity(valueOf(index), nested(index)))
      .forEach(entityRepository::save);
  }

  @GetMapping("/entities")
  public void getEntities() {
    int numberOfEntities = (int) entityRepository.count();

    ClassLoader invokerContextClassLoader = Thread.currentThread().getContextClassLoader();

    range(0, numberOfEntities)
      .parallel()
      .forEach(index -> {
        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();

        try {
          if (enableWorkaround) {
            Thread.currentThread().setContextClassLoader(invokerContextClassLoader);
          }

          try {
            entityRepository.findById(valueOf(index)).ifPresent(EntityController::logSuccess);
          } catch (Exception e) {
            logError(e);
          }
        } finally {
          if (enableWorkaround) {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
          }
        }
      });
  }

  private static void logSuccess(Entity entity) {
    System.out.println("Loaded entity " + entity + "\n"
      + "  context class loader : " + threadContextClassLoaderName() + "\n"
      + "  used class loader    : " + usedClassLoader());
  }

  private static void logError(Exception reason) {
    System.err.println("Failed to entity: " + reason.getMessage() + "\n"
      + "  context class loader : " + threadContextClassLoaderName() + "\n"
      + "  used class loader    : " + usedClassLoader());

    reason.printStackTrace();
  }

  private static String threadContextClassLoaderName() {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    return contextClassLoader != null ? contextClassLoader.getClass().getName() : "<none>";
  }

  private static String usedClassLoader() {
    ClassLoader defaultClassLoader = ClassUtils.getDefaultClassLoader();
    return defaultClassLoader != null ? defaultClassLoader.getClass().getName() : "<none>";
  }

  private static Entity.Nested nested(int index) {
    if (index % 2 == 0) {
      return new Entity.Nested.NestedA();
    } else {
      return new Entity.Nested.NestedB();
    }
  }
}
