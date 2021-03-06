/*
   Copyright (c) 2012 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.linkedin.data;

import com.linkedin.data.codec.DataLocation;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.SchemaParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import static org.testng.Assert.assertTrue;


public class TestUtil
{
  static public final PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out));

  static public String stringFromException(Exception e)
  {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintStream printStream = new PrintStream(outputStream);
    e.printStackTrace(printStream);
    printStream.flush();
    return outputStream.toString();
  }

  static public void dumpBytes(PrintStream printStream, byte bytes[])
  {
    for (int i = 0; i < bytes.length; i++)
    {
      if (i % 16 == 0)
        printStream.printf("%1$06X:", i);
      printStream.print(' ');
      if (bytes[i] < 32 || bytes[i] >= 127)
        printStream.print(' ');
      else
        printStream.print((char) bytes[i]);
      printStream.printf(" %1$02X", bytes[i]);
      if (i % 16 == 15)
        printStream.println();
    }
  }

  static public List<Object> asList(Object... objects)
  {
    ArrayList<Object> list = new ArrayList<Object>();
    for (Object object : objects)
    {
      list.add(object);
    }
    return list;
  }

  @SuppressWarnings("unchecked")
  static public <V> Map<String, V> asMap(Object... objects)
  {
    int index = 0;
    String key = null;
    HashMap<String,V> map = new HashMap<String,V>();
    for (Object object : objects)
    {
      if (index % 2 == 0)
      {
        key = (String) object;
      }
      else
      {
        map.put(key, (V) object);
      }
      index++;
    }
    return map;
  }

  static public InputStream inputStreamFromString(String s) throws UnsupportedEncodingException
  {
    byte[] bytes = s.getBytes(Data.UTF_8_CHARSET);
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    return bais;
  }

  static public SchemaParser schemaParserFromString(String s) throws UnsupportedEncodingException, IOException
  {
    SchemaParser parser = new SchemaParser();
    parser.parse(inputStreamFromString(s));
    return parser;
  }

  static public SchemaParser schemaParserFromObjects(List<Object> objects) throws IOException
  {
    SchemaParser parser = new SchemaParser();
    parser.parse(objects);
    return parser;
  }

  static public SchemaParser schemaParserFromObjectsString(String stringOfObjects) throws IOException
  {
    List<Object> objects = objectsFromString(stringOfObjects);
    return schemaParserFromObjects(objects);
  }

  static public DataSchema dataSchemaFromString(String s) throws IOException
  {
    SchemaParser parser = schemaParserFromString(s);
    if (parser.hasError())
    {
      out.println("ERROR: " + parser.errorMessage());
      return null;
    }
    return parser.topLevelDataSchemas().get(parser.topLevelDataSchemas().size() - 1);
  }

  private static final JacksonDataCodec codec = new JacksonDataCodec();

  public static List<Object> objectsFromString(String string) throws IOException
  {
    return objectsFromInputStream(inputStreamFromString(string));
  }

  public static List<Object> objectsFromInputStream(InputStream inputStream) throws IOException
  {
    StringBuilder errorMessageBuilder = new StringBuilder();
    List<Object> objects = codec.parse(inputStream, errorMessageBuilder, new HashMap<Object, DataLocation>());
    if (errorMessageBuilder.length() > 0)
    {
      throw new IOException(errorMessageBuilder.toString());
    }
    return objects;
  }

  public static DataMap dataMapFromString(String json) throws IOException
  {
    return codec.stringToMap(json);
  }

  public static boolean deleteRecursive(String path, boolean debug) throws FileNotFoundException
  {
    return deleteRecursive(new File(path), debug);
  }

  public static void ensureEmptyOutputDir(File dir, boolean debug) throws FileNotFoundException
  {
    deleteRecursive(dir, debug);
    dir.mkdirs();
    assertTrue(dir.exists());
    assertTrue(dir.isDirectory());
    assertTrue(dir.canWrite());
  }

  public static boolean deleteRecursive(File path, boolean debug) throws FileNotFoundException
  {
    if (!path.exists())
    {
      return true;
    }
    boolean ret = true;
    if (path.isDirectory())
    {
      for (File f : path.listFiles())
      {
        ret = ret && deleteRecursive(f, debug);
      }
    }
    if (debug) out.println("deleting " + path);
    return ret && path.delete();
  }

  public static File testDir(String testName, boolean debug) throws IOException
  {
    File currentDir = new File(".");
    String currentDirName = currentDir.getCanonicalPath();
    if (debug) out.println(currentDir.getCanonicalPath());
    String testDirName = currentDirName +
                         File.separator + "build" +
                         File.separator + "test" +
                         File.separator +
                         testName.replace("/", File.separator);
    if (debug) out.println(testDirName);
    return new File(testDirName);
  }

  public static Map<File, Map.Entry<String, String>> createSchemaFiles(File testDir, Map<String, String> fileToSchemaMap, boolean debug) throws IOException
  {
    Map<File, Map.Entry<String,String>> result = new HashMap<File, Map.Entry<String, String>>();

    ensureEmptyOutputDir(testDir, debug);

    // create test files
    for (Map.Entry<String,String> entry : fileToSchemaMap.entrySet())
    {
      String filename = (testDir.getCanonicalPath() + entry.getKey()).replace('/', File.separatorChar);
      File file = new File(filename);
      if (debug) out.println("creating " + file);
      File parentFile = file.getParentFile();
      parentFile.mkdirs();
      FileOutputStream outputStream = new FileOutputStream(file);
      outputStream.write(entry.getValue().getBytes(Data.UTF_8_CHARSET));
      outputStream.close();
      result.put(file, entry);
    }
    return result;
  }

  public static void createSchemaJar(String jarFileName, Map<String, String> fileToSchemaMap, boolean debug) throws IOException
  {
    if (debug) out.println("creating " + jarFileName);
    FileOutputStream jarFileStream = new FileOutputStream(jarFileName);
    JarOutputStream jarStream = new JarOutputStream(jarFileStream, new Manifest());
    for (Map.Entry<String,String> entry : fileToSchemaMap.entrySet())
    {
      String key = entry.getKey();
      String filename = "pegasus" + key.replace('/', File.separatorChar);
      if (debug) out.println("  adding " + filename);
      JarEntry jarEntry = new JarEntry(filename);
      jarStream.putNextEntry(jarEntry);
      jarStream.write(entry.getValue().getBytes(Data.UTF_8_CHARSET));
    }
    jarStream.close();
    jarFileStream.close();
  }

  public static Collection<String> computePathFromRelativePaths(File testDir, Collection<String> relativePaths) throws IOException
  {
    Collection<String> paths = new ArrayList<String>();

    // directory in path
    for (String testPath : relativePaths)
    {
      String dirname = (testDir.getCanonicalPath() + testPath).replace('/', File.separatorChar);
      paths.add((new File(dirname)).getCanonicalPath());
    }
    return paths;
  }

  public static Collection<String> createJarsFromRelativePaths(File testDir,
                                                               Map<String, String> fileToSchemaMap,
                                                               Collection<String> relativePaths,
                                                               boolean debug)
    throws IOException
  {
    Collection<String> paths = new ArrayList<String>();

    // jar files in path, create jar files
    paths.clear();
    for (String testPath : relativePaths)
    {
      String jarFileName = (testDir.getCanonicalPath() + testPath + ".jar").replace('/', File.separatorChar);
      Map<String,String> jarFileContents = new HashMap<String, String>();
      for (Map.Entry<String,String> entry : fileToSchemaMap.entrySet())
      {
        if (entry.getKey().startsWith(testPath))
        {
          String key = entry.getKey();
          jarFileContents.put(key.substring(testPath.length()), entry.getValue());
        }
      }
      TestUtil.createSchemaJar(jarFileName, jarFileContents, debug);
      paths.add(jarFileName);
    }
    return paths;
  }

  public static String pathsToString(Collection<String> paths)
  {
    boolean first = true;
    StringBuilder sb = new StringBuilder();
    for (String path : paths)
    {
      if (!first) sb.append(":");
      sb.append(path);
      first = false;
    }
    return sb.toString();
  }

  public static void checkEachLineStartsWithLocation(String message)
  {
    String lines[] = message.split("\n");
    for (String line : lines)
    {
      assertTrue(LOCATION_REGEX.matcher(line).matches(), "\"" + line + "\" does not start with location");
    }
  }

  private static final Pattern LOCATION_REGEX = Pattern.compile("^\\d+,\\d+: .*$");
}
