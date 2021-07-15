package io.fixprotocol.orchestra2md;

import java.math.BigInteger;
import java.util.List;
import io.fixprotocol._2020.orchestra.repository.CodeSetType;
import io.fixprotocol._2020.orchestra.repository.ComponentType;
import io.fixprotocol._2020.orchestra.repository.FieldType;
import io.fixprotocol._2020.orchestra.repository.GroupType;
import io.fixprotocol._2020.orchestra.repository.Repository;

final class RepositoryAdaptor {

  static CodeSetType findCodesetByName(Repository repository, String name, String scenario) {
    final List<CodeSetType> codesets = repository.getCodeSets().getCodeSet();
    for (final CodeSetType codeset : codesets) {
      if (codeset.getName().equals(name) && codeset.getScenario().equals(scenario)) {
        return codeset;
      }
    }
    return null;
  }

  static ComponentType findComponentByTag(Repository repository, BigInteger tag, String scenario) {
    final List<ComponentType> components = repository.getComponents().getComponent();
    for (final ComponentType component : components) {
      if (component.getId().equals(tag) && component.getScenario().equals(scenario)) {
        return component;
      }
    }
    return null;
  }

  static FieldType findFieldByTag(Repository repository, BigInteger tag, String scenario) {
    final List<FieldType> fields = repository.getFields().getField();
    for (final FieldType field : fields) {
      if (field.getId().equals(tag) && field.getScenario().equals(scenario)) {
        return field;
      }
    }
    return null;
  }

  static GroupType findGroupByTag(Repository repository, BigInteger tag, String scenario) {
    final List<GroupType> groups = repository.getGroups().getGroup();
    for (final GroupType group : groups) {
      if (group.getId().equals(tag) && group.getScenario().equals(scenario)) {
        return group;
      }
    }
    return null;
  }

}
