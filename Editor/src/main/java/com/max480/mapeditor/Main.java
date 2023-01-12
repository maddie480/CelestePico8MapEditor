package com.max480.mapeditor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Main {
    private static int currentChoiceIndex;
    private static LinkedList<String> pastStates;
    private static LinkedList<String> futureStates;

    public static void main(String[] args) throws IOException {
        currentChoiceIndex = 0;
        pastStates = new LinkedList<>();
        futureStates = new LinkedList<>();

        final String graphicsFile, tilemapFile;
        String loadedGraphicsFile = null, loadedTilemapFile = null;
        boolean vanillaGraphics = false;

        // load previous settings if present.
        if (new File("picomapeditor-settings.txt").exists()) {
            String s = FileUtils.readFileToString(new File("picomapeditor-settings.txt"), UTF_8);
            loadedGraphicsFile = s.split("\n")[0];
            loadedTilemapFile = s.split("\n")[1];
        }

        // ask for the graphics file
        if (loadedGraphicsFile != null && new File(loadedGraphicsFile).isFile()
                && JOptionPane.showConfirmDialog(null, "Use same PICO-8 atlas graphics file as last launch?\n" + loadedGraphicsFile, "Mini PICO-8 Map Editor", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            graphicsFile = loadedGraphicsFile;
        } else if (JOptionPane.showConfirmDialog(null, "Do you want to use vanilla graphics?", "Mini PICO-8 Map Editor", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            Path vanillaGraphicsPath = Files.createTempFile("vanilla-atlas-", ".png");
            try (InputStream is = Main.class.getResourceAsStream("/vanilla-atlas.png");
                 OutputStream os = Files.newOutputStream(vanillaGraphicsPath)) {

                IOUtils.copy(is, os);
            }

            graphicsFile = vanillaGraphicsPath.toAbsolutePath().toString();
            vanillaGraphics = true;
        } else {
            graphicsFile = chooseFile("Choose the PICO-8 atlas graphics file", "png", false);
            if (graphicsFile == null) return;
        }

        // ask for the tilemap file
        if (loadedTilemapFile != null && new File(loadedTilemapFile).isFile()
                && JOptionPane.showConfirmDialog(null, "Use same map file as last launch?\n" + loadedTilemapFile, "Mini PICO-8 Map Editor", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            tilemapFile = loadedTilemapFile;
        } else if (JOptionPane.showConfirmDialog(null, "Do you want to create a new map?", "Mini PICO-8 Map Editor", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            tilemapFile = chooseFile("Choose the path to the new tilemap file", "txt", true);
            if (tilemapFile == null) return;

            try (InputStream is = Main.class.getResourceAsStream("/vanilla-tilemap.txt");
                 OutputStream os = Files.newOutputStream(Paths.get(tilemapFile))) {

                IOUtils.copy(is, os);
            }
        } else {
            tilemapFile = chooseFile("Choose the map file", "txt", false);
            if (tilemapFile == null) return;
        }

        FileUtils.writeStringToFile(new File("picomapeditor-settings.txt"),
                (vanillaGraphics ? "" : graphicsFile) + "\n" + tilemapFile, UTF_8);

        // load the tileset/graphics
        BufferedImage tilesetImage = ImageIO.read(new File(graphicsFile));
        BufferedImage[] tiles = new BufferedImage[tilesetImage.getWidth() / 8 * (tilesetImage.getHeight() / 8)];
        for (int i = 0; i < tilesetImage.getHeight() / 8; i++) {
            for (int j = 0; j < tilesetImage.getWidth() / 8; j++) {
                int imageIndex = j + i * (tilesetImage.getWidth() / 8);
                tiles[imageIndex] = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                tiles[imageIndex].getGraphics().drawImage(tilesetImage, 0, 0, 16, 16, j * 8, i * 8, j * 8 + 8, i * 8 + 8, null);
            }
        }

        BufferedImage tilesetImageTwiceAsBig = new BufferedImage(tilesetImage.getWidth() * 2, tilesetImage.getHeight() * 2, BufferedImage.TYPE_INT_ARGB);
        tilesetImageTwiceAsBig.getGraphics().drawImage(tilesetImage, 0, 0, tilesetImageTwiceAsBig.getWidth(), tilesetImageTwiceAsBig.getHeight(), null);

        // render the map
        BufferedImage result = renderPico8(tiles, tilemapFile);

        // display the map window
        JFrame mapWindow = new JFrame("Mini PICO-8 Map Editor by max480 / Ctrl+Z to undo, Ctrl+Y to redo");
        JLabel mapImage = new JLabel(new ImageIcon(result));
        JPanel black = new JPanel();
        black.add(mapImage);
        black.setBackground(Color.BLACK);
        black.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        JScrollPane scrollPane = new JScrollPane(black);
        scrollPane.setBackground(Color.BLACK);
        scrollPane.setWheelScrollingEnabled(false);
        mapWindow.add(scrollPane);
        mapWindow.setBounds(0, 0, 1024, 512);
        mapWindow.setLocationRelativeTo(null);
        mapWindow.setVisible(true);
        mapWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // display the tilemap window
        JFrame tilemapWindow = new JFrame("Tile Chooser");
        tilemapWindow.getContentPane().setBackground(Color.BLACK);
        tilemapWindow.setLayout(new BorderLayout());
        JLabel tilesetImageLabel = new JLabel(new ImageIcon(tilesetImageTwiceAsBig));
        tilemapWindow.add(tilesetImageLabel);
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        JLabel currentText = new JLabel("Current:");
        currentText.setForeground(Color.WHITE);
        panel.add(currentText);
        panel.setOpaque(false);
        JLabel currentChoice = new JLabel(new ImageIcon(tiles[0]));
        panel.add(currentChoice);
        tilemapWindow.add(panel, BorderLayout.SOUTH);
        tilemapWindow.pack();
        tilemapWindow.setLocationRelativeTo(null);
        tilemapWindow.setBounds(mapWindow.getX() + mapWindow.getWidth() + 10, tilemapWindow.getY(), tilemapWindow.getWidth(), tilemapWindow.getHeight());
        tilemapWindow.setVisible(true);
        tilemapWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        tilemapWindow.setResizable(false);
        tilemapWindow.setAlwaysOnTop(true);

        // when we click on the tileset window, we should pick another tile.
        tilesetImageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                currentChoiceIndex = e.getX() / 16 + (e.getY() / 16) * (tilesetImage.getWidth() / 8);
                currentChoice.setIcon(new ImageIcon(tiles[currentChoiceIndex]));
            }
        });

        // when we click on the map window, we should edit the map.
        mapImage.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                try {
                    // read the tilemap.
                    String input = FileUtils.readFileToString(new File(tilemapFile), UTF_8);
                    pastStates.push(input);
                    futureStates.clear();
                    if (pastStates.size() > 100) pastStates.removeLast();

                    // compute the tile to edit.
                    int index = e.getX() / 16 + (e.getY() / 16) * 128;

                    // compute the value of the tile.
                    String hex = Integer.toHexString(currentChoiceIndex);
                    if (hex.length() == 1) hex = "0" + hex;
                    if (index * 2 > input.length() / 2) {
                        // tiles past the first half of the file are read backwards for some reason.
                        hex = hex.charAt(1) + "" + hex.charAt(0);
                    }

                    // edit the tilemap and save it.
                    input = input.substring(0, index * 2) + hex + input.substring(index * 2 + 2);
                    FileUtils.writeStringToFile(new File(tilemapFile), input, UTF_8);

                    // re-render it.
                    mapImage.setIcon(new ImageIcon(renderPico8(tiles, tilemapFile)));
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    JOptionPane.showMessageDialog(mapWindow, "Saving the map failed!\n" + ioException.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // handle Undo and Redo.
        KeyListener undoRedoListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                try {
                    if (e.getKeyCode() == KeyEvent.VK_Z && e.isControlDown() && !pastStates.isEmpty()) {
                        // Ctrl+Z: undo.
                        futureStates.push(FileUtils.readFileToString(new File(tilemapFile), UTF_8));
                        FileUtils.writeStringToFile(new File(tilemapFile), pastStates.pop(), UTF_8);
                        mapImage.setIcon(new ImageIcon(renderPico8(tiles, tilemapFile)));
                        if (futureStates.size() > 100) futureStates.removeLast();
                    }
                    if (e.getKeyCode() == KeyEvent.VK_Y && e.isControlDown() && !futureStates.isEmpty()) {
                        // Ctrl+Y: redo.
                        pastStates.push(FileUtils.readFileToString(new File(tilemapFile), UTF_8));
                        FileUtils.writeStringToFile(new File(tilemapFile), futureStates.pop(), UTF_8);
                        mapImage.setIcon(new ImageIcon(renderPico8(tiles, tilemapFile)));
                        if (pastStates.size() > 100) pastStates.removeLast();
                    }
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    JOptionPane.showMessageDialog(mapWindow, "Saving the map failed!\n" + ioException.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        mapWindow.addKeyListener(undoRedoListener);
        tilemapWindow.addKeyListener(undoRedoListener);

        // if I don't re-pack() after setting resizable to false on Java 8 I'm getting margins throwing my calculations off apparently?
        // oh well.
        tilemapWindow.pack();
    }

    private static String chooseFile(String title, String extension, boolean newFile) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith("." + extension);
            }

            @Override
            public String getDescription() {
                return "*." + extension;
            }
        });
        int result = chooser.showDialog(null, "Choose");
        if (result == JFileChooser.APPROVE_OPTION) {
            // choosing existing file that does not exist
            if (!newFile && !chooser.getSelectedFile().exists()) {
                JOptionPane.showMessageDialog(null, "The specified file does not exist!", "Mini PICO-8 Map Editor", JOptionPane.ERROR_MESSAGE);
                return chooseFile(title, extension, newFile);
            }

            // choosing new file that already exists
            if (newFile && chooser.getSelectedFile().exists()
                    && JOptionPane.showConfirmDialog(null, "This file already exists. Do you want to overwrite it?", "Mini PICO-8 Map Editor", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION) {

                return chooseFile(title, extension, newFile);
            }

            return chooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

    private static BufferedImage renderPico8(BufferedImage[] tiles, String tilemapFilename) throws IOException {
        // read the tilemap in the same ~~convoluted~~ way as Celeste
        int[] tilemap;
        String input = FileUtils.readFileToString(new File(tilemapFilename), UTF_8);
        input = input.replaceAll("\\s+", "");
        tilemap = new int[input.length() / 2];
        int length = input.length();
        for (int i = 0; i < length; i += 2) {
            char c1 = input.charAt(i);
            char c2 = input.charAt(i + 1);
            tilemap[i / 2] = Integer.parseInt((i < length / 2) ? (c1 + "" + c2) : (c2 + "" + c1), 16);
        }

        // paint the tiles on the image
        BufferedImage result = new BufferedImage(2048, 1024, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < tilemap.length; i++) {
            result.getGraphics().drawImage(tiles[tilemap[i]], (i % 128) * 16, (i / 128) * 16, null);
        }
        return result;
    }
}
