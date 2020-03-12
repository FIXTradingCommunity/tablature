package io.fixprotocol.md.event.mutable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Stream;
import io.fixprotocol.md.event.Context;
import io.fixprotocol.md.event.DetailProperties;
import io.fixprotocol.md.event.MutableDetailProperties;
import io.fixprotocol.md.event.MutableDetailTable;

public class DetailTableImpl extends DocumentationImpl implements MutableDetailTable {
  
  private class DetailPropertiesImpl implements MutableDetailProperties {
    
    private final Map<String, String> properties = new LinkedHashMap<>();

    @Override
    public void addProperty(String key, String value) {
      properties.put(key.toLowerCase(), value);
    }

    public Stream<Entry<String, String>> getProperties() {
      return properties.entrySet().stream();
    }

    @Override
    public String getProperty(String key) {
      return properties.get(key.toLowerCase());
    }
    
    @Override
    public String toString() {
      return "DetailPropertiesImpl [properties=" + properties + "]";
    }

    @Override
    public Context getContext() {
      return DetailTableImpl.this;
    }  
  }

  private final List<DetailProperties> propertiesList = new ArrayList<>();
  
  public DetailTableImpl() {
    this(EMPTY_CONTEXT, 0);
  }

  public DetailTableImpl(String[] keys) {
    this(keys, 0);
  }
  
  public DetailTableImpl(int level) {
    super(EMPTY_CONTEXT, level);
  }

  public DetailTableImpl(String[] keys, int level) {
    super(keys, level);
  }
  
  @Override
  public DetailProperties addProperties(DetailProperties detailProperties) {
    propertiesList.add(detailProperties);
    return detailProperties;
  }
  
  @Override
  public Supplier<Stream<? extends DetailProperties>> rows() {
    return () -> propertiesList.stream();
   
  }

  @Override
  public MutableDetailProperties newRow() {
    final DetailPropertiesImpl detailProperties = new DetailPropertiesImpl();
    addProperties(detailProperties);
    return detailProperties;
  }

  @Override
  public Collection<TableColumnImpl> getTableColumns() {
    final Map<String, TableColumnImpl> columns = new LinkedHashMap<>();

    rows().get().forEach(r -> r.getProperties().forEach(p -> {
      final String key = p.getKey();
      TableColumnImpl column = columns.get(key);
      if (column == null) {
        columns.put(key, new TableColumnImpl(key, Math.max(key.length(), p.getValue().length())));
      } else {
        column.updateLength(p.getValue().length());
      }
    }));

    return columns.values();
  }

}
