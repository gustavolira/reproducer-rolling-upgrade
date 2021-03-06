import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

public class Author {
   @ProtoField(number = 1)
   final String name;

   @ProtoField(number = 2)
   final String surname;

   @ProtoFactory
   Author(String name, String surname) {
      this.name = name;
      this.surname = surname;
   }

   public String getName() {
      return name;
   }

   public String getSurname() {
      return surname;
   }

   public String getSurname2() {
      return surname;
   }

}