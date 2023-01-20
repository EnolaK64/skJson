/**
 *   This file is part of skJson.
 * <p>
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * <p>
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * <p>
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Copyright coffeeRequired nd contributors
 */
package cz.coffee.utils.config;

import cz.coffee.SkJson;
import cz.coffee.utils.ErrorHandler;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cz.coffee.utils.ErrorHandler.sendMessage;

@SuppressWarnings("unused")
public class Config {
    private static final File configFile = new File("plugins" + File.separator + SkJson.getInstance().getDescription().getName() + File.separator + "config.yml");

    public static double _CONFIG_VERSION;
    public static boolean _DEBUG = false;
    public static boolean _EXAMPLES;
    public static boolean _REQUEST_HANDLER;
    public static List<Object> _HANDLERS_REQUEST;

    private static DumperOptions dumperOptions() {
        DumperOptions o = new DumperOptions();
        o.setIndent(4);
        o.setPrettyFlow(true);
        o.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        o.setAllowUnicode(true);
        return o;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void init(){
        if (!configFile.exists()) {
            try {
                if (!new File(configFile.getParent()).exists()) {
                    new File(configFile.getParent()).mkdirs();
                }
                configFile.createNewFile();
                writeDefault();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        load();

    }

    private static void writeDefault() {
        Yaml yml = new Yaml(dumperOptions());
        Map<String,Object> map = null;
        if (configFile.length() > 1) {
            try {
                map = yml.load(new FileInputStream(configFile));
                map.put("version", 2.0);
                map.put("debug", false);
                map.put("create-examples", false);
                map.put("handle-request", true);
                map.put("handlers", List.of("SkriptWebApi","Reqn","reflect"));
            } catch (IOException exception){
                sendMessage(exception.getMessage(), ErrorHandler.Level.ERROR);
            }
        } else {
            map = new HashMap<>();
            map.put("version", 2.0);
            map.put("debug", false);
            map.put("create-examples", false);
            map.put("handle-request", true);
            map.put("handlers", List.of("SkriptWebApi","Reqn","reflect"));
        }
        try (PrintWriter pw = new PrintWriter(configFile)) {
            yml.dump(map, pw);
            pw.flush();
            pw.close();
            writeComments();
        } catch (IOException exception){
            sendMessage(exception.getMessage(), ErrorHandler.Level.ERROR);
        }
    }

    private static void writeComments() {
        try {
            for (String c : new String[]{"do not change the 'version'"}) {
                String comment = "# " + c;
                Files.write(configFile.toPath(), comment.getBytes(), StandardOpenOption.APPEND);
            }
        } catch (IOException ioException) {
            sendMessage(ioException.getMessage(), ErrorHandler.Level.ERROR);
        }
    }



    public static void load() {
        Yaml yml = new Yaml(dumperOptions());
        FileInputStream fis;
        try {
            fis = new FileInputStream(configFile);
            Map<String, Object> map = yml.load(fis);
            _CONFIG_VERSION = Double.parseDouble(map.get("version").toString());
            _DEBUG = Boolean.parseBoolean(map.get("debug").toString());
            _EXAMPLES = Boolean.parseBoolean(map.get("create-examples").toString());
            _REQUEST_HANDLER = Boolean.parseBoolean(map.get("handle-request").toString());
            //noinspection unchecked
            _HANDLERS_REQUEST = (ArrayList<Object>) map.get("handlers");
            fis.close();
        } catch (IOException exception) {
            sendMessage(exception.getMessage(), ErrorHandler.Level.ERROR);
        }
    }

}
