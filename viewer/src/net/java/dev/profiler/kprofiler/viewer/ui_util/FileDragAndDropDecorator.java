package net.java.dev.profiler.kprofiler.viewer.ui_util;

import java.awt.*;
import static java.awt.datatransfer.DataFlavor.javaFileListFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Handles drag&amp;drop of a file.
 *
 * @author Kohsuke Kawaguchi
 */
public final class FileDragAndDropDecorator extends DropTargetAdapter {
    /**
     * Decorates the given {@link Component} so that it can accept
     * a file DnD.
     */
    public static void decorate(Component component, Listener listener) {
        new DropTarget(component,DnDConstants.ACTION_COPY_OR_MOVE,new FileDragAndDropDecorator(listener),true);
    }

    /**
     * Implemented by the client of this class to execute the action when a file is dropped.
     */
    public interface Listener {
        /**
         *
         * @return true
         *      if the operation is successful, otherwise false.
         */
        boolean onDrop( File file );
    }

    private final Listener listener;

    public FileDragAndDropDecorator(Listener listener) {
        this.listener = listener;
    }

    public void dragEnter(DropTargetDragEvent e) {
        if(isFormatOK(e))
            e.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
        else
            e.rejectDrag();
    }

    private boolean isFormatOK(DropTargetDragEvent e) {
        List<File> files = getFileList(e.getTransferable());
        if(files.size()!=1)
            return false;

        return true;
    }

    private List<File> getFileList(Transferable t) {
        try {
            return (List<File>) t.getTransferData(javaFileListFlavor);
        } catch (UnsupportedFlavorException e) {
            return Collections.emptyList();
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public void drop(DropTargetDropEvent e) {
        e.acceptDrop(DnDConstants.ACTION_COPY);

        List<File> files = getFileList(e.getTransferable());

        e.dropComplete(listener.onDrop(files.get(0)));
    }

}
