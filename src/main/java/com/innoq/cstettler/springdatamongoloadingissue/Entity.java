package com.innoq.cstettler.springdatamongoloadingissue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public class Entity {

  @Id
  private String id;
  private Nested nested;

  interface Nested {

    @Data
    @BsonDiscriminator(
      key = "_class"
    )
    class NestedA implements Nested {

    }

    @Data
    @BsonDiscriminator(
      key = "_class"
    )
    class NestedB implements Nested {

    }
  }
}
