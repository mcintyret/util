package com.mcintyret.utils.serialize;

import com.google.common.collect.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mcintyret.utils.io.RuntimeIoException;

import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * User: mcintyret2
 * Date: 10/05/2013
 */
public class JsonSerializer extends AbstractWriterSerializer {

    private GsonBuilder gsonBuilder = new GsonBuilder()
        .registerTypeAdapterFactory(new TypeAdapterFactory() {
            @Override
            public <T> TypeAdapter<T> create(final Gson gson, TypeToken<T> type) {
                if (Multiset.class.isAssignableFrom(type.getRawType())) {
                    return (TypeAdapter<T>) new TypeAdapter<Multiset>() {
                        @Override
                        public void write(JsonWriter out, Multiset value) throws IOException {
                            Map<Object, Integer> map = Maps.newHashMapWithExpectedSize(value.elementSet().size());
                            for (Object o : value.elementSet()) {
                                map.put(o, value.count(o));
                            }
                            gson.toJson(map, Map.class, out);
                        }

                        @Override
                        public Multiset read(JsonReader in) throws IOException {
                            Map<Object, Integer> map = gson.getAdapter(new TypeToken<Map<Object, Integer>>() {
                            }).read(in);
                            Multiset multiset = HashMultiset.create();
                            for (Map.Entry<Object, Integer> entry : map.entrySet()) {
                                multiset.add(entry.getKey(), entry.getValue());
                            }
                            return multiset;
                        }
                    };
                } else if (Multimap.class.isAssignableFrom(type.getRawType())) {

                    return (TypeAdapter<T>) new TypeAdapter<Multimap>() {

                        @Override
                        public void write(JsonWriter out, Multimap value) throws IOException {
                            gson.toJson(value.asMap(), Map.class, out);
                        }

                        @Override
                        public Multimap read(JsonReader in) throws IOException {
                            Map<Object, List<Object>> map = gson.getAdapter(new TypeToken<Map<Object, List<Object>>>() {
                            }).read(in);
                            Multimap multimap = ArrayListMultimap.create();
                            for (Map.Entry<Object, List<Object>> entry : map.entrySet()) {
                                multimap.putAll(entry.getKey(), entry.getValue());
                            }
                            return multimap;
                        }
                    };
                } else {
                    return null;
                }
            }
        });

    private Gson gson;

    @Override
    public void serialize(Object obj, Writer writer) {
        ensureGsonBuilt();
        try (BufferedWriter bw = new BufferedWriter(writer)) {
            gson.toJson(obj, bw);
        } catch (IOException e) {
            throw new RuntimeIoException(e);
        }
    }

    @Override
    public <T> T deserialize(Reader reader, Class<T> clazz) {
        ensureGsonBuilt();
        try (BufferedReader br = new BufferedReader(reader))  {
            return gson.fromJson(br, clazz);
        } catch (IOException e) {
            throw new RuntimeIoException(e);
        }
    }

    @Override
    public String getSuffix() {
        return ".json";
    }

    public GsonBuilder configure() {
        return gsonBuilder;
    }

    private void ensureGsonBuilt() {
        if (gson == null) {
            gson = gsonBuilder.create();
        }
    }
}
