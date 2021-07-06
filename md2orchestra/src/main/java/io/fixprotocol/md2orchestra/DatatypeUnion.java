package io.fixprotocol.md2orchestra;

import io.fixprotocol._2020.orchestra.repository.CodeSetType;
import io.fixprotocol._2020.orchestra.repository.Datatype;

/**
 * A discriminated union of datatype or codeset
 * 
 * @author Don Mendelson
 *
 */
class DatatypeUnion {
  enum Discriminator {
    CODESET,
    DATATYPE
  }
  private final CodeSetType codeset;
  private final io.fixprotocol._2020.orchestra.repository.Datatype datatype;  
  private final DatatypeUnion.Discriminator discriminator;
  
  public DatatypeUnion(CodeSetType codeset) {
    this(null, codeset, Discriminator.CODESET);
  }

  public DatatypeUnion(Datatype datatype) {
    this(datatype, null, Discriminator.DATATYPE);
  }

  private DatatypeUnion(Datatype datatype, CodeSetType codeset, DatatypeUnion.Discriminator discriminator) {
    this.datatype = datatype;
    this.codeset = codeset;
    this.discriminator = discriminator;
  }

  public CodeSetType getCodeset() {
    return codeset;
  }
  
  public io.fixprotocol._2020.orchestra.repository.Datatype getDatatype() {
    return datatype;
  }

  public DatatypeUnion.Discriminator getDiscriminator() {
    return discriminator;
  }
}