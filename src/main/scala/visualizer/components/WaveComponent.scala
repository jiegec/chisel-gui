package visualizer.components

import java.awt.{Color, Rectangle}

import scalaswingcontrib.tree.Tree
import visualizer._
import visualizer.controllers.{SelectionController, WaveFormController}
import visualizer.models._
import visualizer.painters.{MultiBitPainter, ReadyValidPainter, SingleBitPainter}

import scala.swing._
import scala.swing.event._

class WaveComponent(waveFormController: WaveFormController) extends BorderPanel {

  preferredSize = new Dimension (550, 700)

  ///////////////////////////////////////////////////////////////////////////
  // View
  ///////////////////////////////////////////////////////////////////////////
  private val multiBitPainter = new MultiBitPainter(waveFormController)
  private val singleBitPainter = new SingleBitPainter(waveFormController)
  private val readyValidPainter = new ReadyValidPainter(waveFormController)

  def tree: Tree[SelectionNode] = waveFormController.tree

  override def paintComponent(g: Graphics2D): Unit = {
    super.paintComponent(g)

    val visibleRect = peer.getVisibleRect

    // Set background color
    background = Color.white

    // Draw waveforms
    TreeHelper.viewableDepthFirstIterator(tree).zipWithIndex.foreach { case (node, row) =>
      val y = row * DrawMetrics.WaveformVerticalSpacing + DrawMetrics.WaveformVerticalGap

      node match {
        case waveSignal: WaveSignal =>
          if(waveSignal.isBinary) {
            singleBitPainter.paintWaveform(g, visibleRect, y, waveSignal.waveform)
          }
          else {
            multiBitPainter.paintWaveform(g, visibleRect, y, waveSignal.waveform)
          }
        case waveGroup: WaveGroup =>
          //TODO: get composite stuff working here
//          readyValidPainter.paintWaveform(g, visibleRect, y, waveGroup.)

      }
//      for {
//        signal <- waveFormController.waveFormDataMap.get(node)
//        painter <- waveFormController.waveDisplaySettings.get(node)
//      } {
//        signal match {
//          case pureSignal: PureSignal if pureSignal.waveform.isDefined =>
//            if (pureSignal.waveform.get.isBinary)
//              singleBitPainter.paintWaveform(g, visibleRect, y, pureSignal)
//            else
//              multiBitPainter.paintWaveform(g, visibleRect, y, pureSignal)
//          case combinedSignal: CombinedSignal =>
//            readyValidPainter.paintWaveform(g, visibleRect, y, combinedSignal)
//          case _ =>
//        }
//      }
//      waveFormController.waveFormDataMap.get(node) match {
//        case Some(pureSignal: PureSignal) =>
//
//          val painter = waveFormController.waveDisplaySettings.getOrElse(node, )
//          waveFormController.waveDisplaySettings(node).painter match {
//            case _ =>
//              if (pureSignal.waveform.get.isBinary)
//                singleBitPainter.paintWaveform(g, visibleRect, y, pureSignal)
//              else
//                multiBitPainter.paintWaveform(g, visibleRect, y, pureSignal)
//          }
//        case Some(combinedSignal: CombinedSignal) =>
//          readyValidPainter.paintWaveform(g, visibleRect, y, combinedSignal)
//        case _ =>
//          // node is a group. do nothing?
//          // or node doesn't have a waveform
//      }
    }

    // Draw markers
    drawMarkers(g, visibleRect)

    // Draw cursor
    g.setColor(new Color(39, 223, 85))
    val cursorX = waveFormController.timestampToXCoordinate(waveFormController.cursorPosition)
    g.drawLine(cursorX, visibleRect.y, cursorX, visibleRect.y + visibleRect.height)
  }

  def drawMarkers(g: Graphics2D, visibleRect: Rectangle): Unit = {
    val startTime = waveFormController.xCoordinateToTimestamp(visibleRect.x)
    val endTime = waveFormController.xCoordinateToTimestamp(visibleRect.x + visibleRect.width)
    val startIndex = waveFormController.getMarkerAtTime(startTime)
    val endIndex = waveFormController.getMarkerAtTime(endTime)

    g.setColor(Color.black)
    waveFormController.markers.slice(startIndex, endIndex + 1).foreach { marker =>
      val x = waveFormController.timestampToXCoordinate(marker.timestamp)
      g.drawLine(x, 0, x, visibleRect.y + visibleRect.height)
    }
  }

  //
  // Helper functions
  //
  def computeBounds(): Unit = {
    preferredSize = new Dimension(waveFormController.timestampToXCoordinate(waveFormController.getMaxTimeStamp),
      TreeHelper.viewableDepthFirstIterator(tree).size
        * DrawMetrics.WaveformVerticalSpacing)
    revalidate()
  }

  ///////////////////////////////////////////////////////////////////////////
  // Controller
  ///////////////////////////////////////////////////////////////////////////
  listenTo(waveFormController, tree)
  listenTo(mouse.clicks, mouse.moves)
  reactions += {
    case _: SignalsChanged | _: ScaleChanged | _: CursorSet | _: MaxTimestampChanged =>
      computeBounds()
      repaint()
    case _: WaveFormatChanged =>
      repaint()
    case e: MarkerChanged =>
      if (e.timestamp < 0)
        repaint()
      else
        repaint(new Rectangle(waveFormController.timestampToXCoordinate(e.timestamp) - 1, 0, 2, peer.getVisibleRect.height))
    case e: MousePressed =>
      val timestamp = waveFormController.xCoordinateToTimestamp(e.peer.getX)
      if (waveFormController.cursorPosition != waveFormController.selectionStart)
        repaint()
      if (!e.peer.isShiftDown)
        waveFormController.selectionStart = timestamp
      waveFormController.setCursorPosition(timestamp)
      // waveFormController.adjustingCursor = true
    case _: MouseReleased => // waveFormController.adjustingCursor = false
    case e: MouseDragged =>
      val timestamp = waveFormController.xCoordinateToTimestamp(e.peer.getX)
      waveFormController.setCursorPosition(timestamp)
      peer.scrollRectToVisible(new Rectangle(e.peer.getX, e.peer.getY, 1, 1))
  }
}