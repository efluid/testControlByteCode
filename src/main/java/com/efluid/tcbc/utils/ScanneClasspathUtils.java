package com.efluid.tcbc.utils;

import java.io.File;
import java.net.*;
import java.security.CodeSource;

import org.slf4j.*;

public class ScanneClasspathUtils {

  private static final Logger LOG = LoggerFactory.getLogger(ScanneClasspathUtils.class);

  /**
   *  Permet de récupérer la location d'une classe (chargé depuis quel jar)
   *  Si cette dernière est inaccessible, généralement chargé par le classloader bootstrap (rt.jar)
   *  Dans ce cas là, on la considère comme une ressource pour pouvoir connaître sa source
   */
  public static URL getClassLocation(Class cls, boolean avecJDK)  {      
    URL url = null;            
  
    CodeSource cs = cls.getProtectionDomain().getCodeSource();
    if (cs != null){
      url = cs.getLocation();
    }
  
    if (url == null && avecJDK){
      ClassLoader clsLoader = cls.getClassLoader();
      String clsAsResource = cls.getName().replace('.', '/').concat(".class");          
      url = clsLoader != null ? clsLoader.getResource (clsAsResource) : ClassLoader.getSystemResource(clsAsResource);
    }
  
    return url;
  }

  /**
   * Permet de convertir les séparateurs d'un chemin avec le même format que l'OS (/ ou \)
   */
  public static String conversionPath(URL location){
    if(location == null) return null;
    String path = null;
    try{
       path = location.getPath();
      if("file".equals(location.getProtocol())){
        path = URLDecoder.decode(path, System.getProperty("file.encoding"));
        // Suppression du premier séparateur et remplacement par le séparateur de l'OS
        path = path.substring(1).replaceAll("/", File.separator.equals("/") ? File.separator : "\\\\");
  
        // Suppression du dernier séparateur pour le répertoire classes
        if(path.lastIndexOf(File.separator)+1 == path.length()){
          path = path.substring(0, path.length()-1);
        }
      } else if("jar".equals(location.getProtocol())){
        path = path.substring(6, path.lastIndexOf("!")).replaceAll("/", File.separator.equals("/") ? File.separator : "\\\\");
      }
    } catch (Throwable ex){
      LOG.error("Error : ", ex);
    }
  
    return path;
  }

  public static String getCheminDeLaClasse(Class aClass){
    return conversionPath(getClassLocation(aClass, false));
  }
}
