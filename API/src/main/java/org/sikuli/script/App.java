/*
 * Copyright 2010-2014, Sikuli.org, sikulix.com
 * Released under the MIT License.
 *
 * modified RaiMan
 */
package org.sikuli.script;

import java.awt.Rectangle;
import org.sikuli.basics.Settings;
import org.sikuli.basics.Debug;
import java.awt.Toolkit;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Map;
import org.sikuli.natives.OSUtil;
import org.sikuli.natives.SysUtil;

/**
 * App implements features to manage (open, switch to, close) applications.
 * on the system we are running on and
 * to access their assets like windows
 * <br>
 * TAKE CARE: function behavior differs depending on the running system
 * (cosult the docs for more info)
 */
public class App {
  
  static RunTime runTime = RunTime.get();

  protected static final OSUtil _osUtil = SysUtil.getOSUtil();
  protected String _appName;
  protected int _pid;
  private static final Map<Type, String> appsWindows; 
  private static final Map<Type, String> appsMac;
  private static final Region aRegion = new Region();

  static {
//TODO Sikuli hangs if App is used before Screen
    new Screen();
		String libName = _osUtil.getLibName();
		if (!libName.isEmpty()) {
			RunTime.loadLibrary(libName);
		}
    appsWindows = new HashMap<Type, String>();
    appsWindows.put(Type.EDITOR, "Notepad");
    appsWindows.put(Type.BROWSER, "Google Chrome");
    appsWindows.put(Type.VIEWER, "");
    appsMac = new HashMap<Type, String>();
    appsMac.put(Type.EDITOR, "TextEdit");
    appsMac.put(Type.BROWSER, "Safari");
    appsMac.put(Type.VIEWER, "Preview");
}
  
  public static enum Type {
    EDITOR, BROWSER, VIEWER
  }
    
  public static Region start(Type appType) {
    App app = null;
    Region win;
    try {
      if (Type.EDITOR.equals(appType)) {
        if (runTime.runningMac) {
          app = new App(appsMac.get(appType));
          if (app.window() != null) {
            app.focus();
            aRegion.wait(0.5);
            win = app.window();
            aRegion.click(win);
            aRegion.write("#M.a#B.");
            return win;
          } else {
            app.open();
            win = app.waitForWindow();
            app.focus();
            aRegion.wait(0.5);
            aRegion.click(win);
            return win;
          }
        }      
        if (runTime.runningWindows) {
          app = new App(appsWindows.get(appType));
          if (app.window() != null) {
            app.focus();
            aRegion.wait(0.5);
            win = app.window();
            aRegion.click(win);
            aRegion.write("#C.a#B.");
            return win;
          } else {
            app.open();
            win = app.waitForWindow();
            app.focus();
            aRegion.wait(0.5);
            aRegion.click(win);
            return win;
          }
        }      
      } else if (Type.BROWSER.equals(appType)) {
        return null;
      } else if (Type.VIEWER.equals(appType)) {
        return null;
      }
    } catch (Exception ex) {}
    return null;
  }
    
  public Region waitForWindow() {
    return waitForWindow(5);
  }

  public Region waitForWindow(int seconds) {
    Region win = null;
    while ((win = window()) == null && seconds > 0) {
      aRegion.wait(0.5);
      seconds -= 0.5;
    }
    return win;
  }

  private static Region asRegion(Rectangle r) {
    if (r != null) {
      return Region.create(r);
    } else {
      return null;
    }
  }

	/**
	 * creates an instance for an app with this name
	 * (nothing done yet)
	 *
	 * @param appName name
	 */
	public App(String appName) {
    _appName = appName;
    _pid = 0;
  }

  protected App(String appName, int pid) {
    _appName = appName;
    _pid = pid;
  }

	/**
	 * creates an instance for an app with this name and tries to open it
	 * @param appName name
	 * @return the App instance or null on failure
	 */
	public static App open(String appName) {
    return (new App(appName)).open();
  }

	/**
	 * tries to identify a running app with the given name
	 * and then tries to close it
	 * @param appName name
	 * @return 0 for success -1 otherwise
	 */
	public static int close(String appName) {
    return _osUtil.closeApp(appName);
  }

	/**
	 * tries to identify a running app with name and
	 * if not running tries to open it
	 * and tries to make it the foreground application
	 * bringing its topmost window to front
	 * @param appName name
	 * @return the App instance or null on failure
	 */
	public static App focus(String appName) {
    return (new App(appName)).focus();
  }

	/**
	 * tries to identify a running app with name and
	 * if not running tries to open it
	 * and tries to make it the foreground application
	 * bringing its window with the given number to front
	 * @param appName name
	 * @param num window
	 * @return the App instance or null on failure
	 */
  public static App focus(String appName, int num) {
    return (new App(appName)).focus(num);
  }

	/**
	 * tries to make it the foreground application
	 * bringing its topmost window to front
	 * @return the App instance or null on failure
	 */
	public App focus() {
    return focus(0);
  }

	/**
	 * tries to make it the foreground application
	 * bringing its window with the given number to front
	 * @param num window
	 * @return the App instance or null on failure
	 */
  public App focus(int num) {
    Debug.action("App.focus " + this.toString() + " #" + num);
    if (_pid != 0) {
      if (_osUtil.switchApp(_pid, num) == 0) {
        Debug.error("App.focus failed: " + _appName
                + "(" + _pid + ") not found");
        return null;
      }
    } else {
      boolean failed = false;
      if (Settings.isWindows()) {
        _pid = _osUtil.switchApp(_appName, num);
        if (_pid == 0) {
          failed = true;
        }
      } else {
        if (_osUtil.switchApp(_appName, num) < 0) {
          failed = true;
        }
      }
      if (failed) {
        Debug.error("App.focus failed: " + _appName + " not found");
        return null;
      }
    }
    return this;
  }

	/**
	 * tries to open the app defined by this App instance
	 * @return this or null on failure
	 */
	public App open() {
    if (Settings.isWindows() || Settings.isLinux()) {
      int pid = _osUtil.openApp(_appName);
      _pid = pid;
      Debug.action("App.open " + this.toString());
      if (pid == 0) {
        Debug.error("App.open failed: " + _appName + " not found");
        return null;
      }
    } else {
      Debug.action("App.open " + this.toString());
      if (_osUtil.openApp(_appName) < 0) {
        Debug.error("App.open failed: " + _appName + " not found");
        return null;
      }
    }
    return this;
  }

	/**
	 * tries to close the app defined by this App instance
	 * @return this or null on failure
	 */
	public int close() {
    Debug.action("App.close " + this.toString());
    if (_pid != 0) {
      int ret = _osUtil.closeApp(_pid);
      if (ret >= 0) {
        return ret;
      }
    }
    return close(_appName);
  }

	/**
	 * the app's name as defined by this App instance
	 * @return the name
	 */
	public String name() {
    return _appName;
  }

	/**
	 * evaluates the region currently occupied
	 * by the topmost window of this App instance.
	 * The region might not be fully visible, not visible at all
	 * or invalid with respect to the current monitor configuration (outside any screen)
	 * @return the region
	 */
	public Region window() {
    if (_pid != 0) {
      return asRegion(_osUtil.getWindow(_pid));
    }
    return asRegion(_osUtil.getWindow(_appName));
  }

	/**
	 * evaluates the region currently occupied
	 * by the window with the given number of this App instance.
	 * The region might not be fully visible, not visible at all
	 * or invalid with respect to the current monitor configuration (outside any screen)
	 * @param winNum window
	 * @return the region
	 */
  public Region window(int winNum) {
    if (_pid != 0) {
      return asRegion(_osUtil.getWindow(_pid, winNum));
    }
    return asRegion(_osUtil.getWindow(_appName, winNum));
  }

	/**
	 * evaluates the region currently occupied by the systemwide frontmost window
	 * (usually the one that has focus for mouse and keyboard actions)
	 * @return the region
	 */
	public static Region focusedWindow() {
    return asRegion(_osUtil.getFocusedWindow());
  }

	/**
	 * evaluates the current textual content of the system clipboard
	 * @return the textual content
	 */
	public static String getClipboard() {
    Transferable content = Clipboard.getSystemClipboard().getContents(null);
    try {
      if (content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        return (String) content.getTransferData(DataFlavor.stringFlavor);
      }
    } catch (UnsupportedFlavorException e) {
      Debug.error("Env.getClipboard: UnsupportedFlavorException: " + content);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "";
  }

	/**
	 * sets the current textual content of the system clipboard to the given text
	 * @param text text
	 */
	public static void setClipboard(String text) {
    Clipboard.putText(Clipboard.PLAIN, Clipboard.UTF8,
            Clipboard.CHAR_BUFFER, text);
  }

  @Override
  public String toString() {
    return _appName + "(" + _pid + ")";
  }

	private static class Clipboard {

   public static final TextType HTML = new TextType("text/html");
   public static final TextType PLAIN = new TextType("text/plain");

   public static final Charset UTF8 = new Charset("UTF-8");
   public static final Charset UTF16 = new Charset("UTF-16");
   public static final Charset UNICODE = new Charset("unicode");
   public static final Charset US_ASCII = new Charset("US-ASCII");

   public static final TransferType READER = new TransferType(Reader.class);
   public static final TransferType INPUT_STREAM = new TransferType(InputStream.class);
   public static final TransferType CHAR_BUFFER = new TransferType(CharBuffer.class);
   public static final TransferType BYTE_BUFFER = new TransferType(ByteBuffer.class);

   private Clipboard() {
   }

   /**
    * Dumps a given text (either String or StringBuffer) into the Clipboard, with a default MIME type
    */
   public static void putText(CharSequence data) {
      StringSelection copy = new StringSelection(data.toString());
      getSystemClipboard().setContents(copy, copy);
   }

   /**
    * Dumps a given text (either String or StringBuffer) into the Clipboard with a specified MIME type
    */
   public static void putText(TextType type, Charset charset, TransferType transferType, CharSequence data) {
      String mimeType = type + "; charset=" + charset + "; class=" + transferType;
      TextTransferable transferable = new TextTransferable(mimeType, data.toString());
      getSystemClipboard().setContents(transferable, transferable);
   }

   public static java.awt.datatransfer.Clipboard getSystemClipboard() {
      return Toolkit.getDefaultToolkit().getSystemClipboard();
   }

   private static class TextTransferable implements Transferable, ClipboardOwner {
      private String data;
      private DataFlavor flavor;

      public TextTransferable(String mimeType, String data) {
         flavor = new DataFlavor(mimeType, "Text");
         this.data = data;
      }

     @Override
      public DataFlavor[] getTransferDataFlavors() {
         return new DataFlavor[]{flavor, DataFlavor.stringFlavor};
      }

     @Override
      public boolean isDataFlavorSupported(DataFlavor flavor) {
         boolean b = this.flavor.getPrimaryType().equals(flavor.getPrimaryType());
         return b || flavor.equals(DataFlavor.stringFlavor);
      }

     @Override
      public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
         if (flavor.isRepresentationClassInputStream()) {
            return new StringReader(data);
         }
         else if (flavor.isRepresentationClassReader()) {
            return new StringReader(data);
         }
         else if (flavor.isRepresentationClassCharBuffer()) {
            return CharBuffer.wrap(data);
         }
         else if (flavor.isRepresentationClassByteBuffer()) {
            return ByteBuffer.wrap(data.getBytes());
         }
         else if (flavor.equals(DataFlavor.stringFlavor)){
            return data;
         }
         throw new UnsupportedFlavorException(flavor);
      }

     @Override
      public void lostOwnership(java.awt.datatransfer.Clipboard clipboard, Transferable contents) {
      }
   }

   /**
    * Enumeration for the text type property in MIME types
    */
   public static class TextType {
      private String type;

      private TextType(String type) {
         this.type = type;
      }

     @Override
      public String toString() {
         return type;
      }
   }

   /**
    * Enumeration for the charset property in MIME types (UTF-8, UTF-16, etc.)
    */
   public static class Charset {
      private String name;

      private Charset(String name) {
         this.name = name;
      }

     @Override
      public String toString() {
         return name;
      }
   }

   /**
    * Enumeration for the transferScriptt type property in MIME types (InputStream, CharBuffer, etc.)
    */
   public static class TransferType {
      private Class dataClass;

      private TransferType(Class streamClass) {
         this.dataClass = streamClass;
      }

      public Class getDataClass() {
         return dataClass;
      }

     @Override
      public String toString() {
         return dataClass.getName();
      }
   }

}

}
