package fireflower
import java.io.{BufferedWriter, FileWriter}
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import au.com.bytecode.opencsv.CSVWriter
import util.control.Breaks._

object PlayerTests {

  def main(args: Array[String]): Unit = {
    val numGames = {
      if (args.length >= 1)
        args(0).toInt
      else
        100
    }

    val numPlayers = {
      if (args.length >= 2)
        args(1).toInt
      else
        2
    }
    //println("NumGames=" + numGames)

    runTests(prefix = "", salt = "g", numGames = numGames, numPlayers = numPlayers)
  }

  def makeRunSeed(name: String, salt: String): Long = {
    RandUtils.sha256Long(RandUtils.sha256Long(name) + salt)
  }

  def runTests(prefix: String, salt: String, numGames: Int, numPlayers: Int): Unit = {
    val start = System.nanoTime()
    var games: List[fireflower.Game] = List[fireflower.Game]()


    if (numPlayers == 3) {
      games = play_game3p(prefix, numGames, salt)
      writeToCsvFile(games, 3)
    } else if (numPlayers == 4) {
      games = play_game4p(prefix, numGames, salt)
      writeToCsvFile(games, 4)
    } else if (numPlayers == 5) {
      games = play_game5p(prefix, numGames, salt)
      writeToCsvFile(games, 5)
    } else {
      games = play_game2p(prefix, numGames, salt)
      writeToCsvFile(games, 2)
    }

    val end = System.nanoTime()

    //println("Done!")
    //println("")
    //println("Time: " + (end - start).toDouble / 1.0e9)
  }

  def play_game2p(prefix: String, numGames: Int, salt: String): List[fireflower.Game] = {
    val rules2p = Rules.Standard(numPlayers = 2, stopEarlyLoss = false)
    val name2p = prefix + "HeuristicStandard2P"
    val games2p = {
      Sim.runMulti(
        name = name2p,
        rules = rules2p,
        numGames,
        runSeed = makeRunSeed(name2p, salt),
        playerGen = HeuristicPlayer,
        doPrint = false,
        doPrintDetails = false,
        useAnsiColors = false
      )
    }

    return games2p
  }

  def play_game3p(prefix: String, numGames: Int, salt: String): List[fireflower.Game] = {
    val rules3p = Rules.Standard(numPlayers = 3, stopEarlyLoss = false)

    val name3p = prefix + "HeuristicStandard3P"
    val games3p = {
      Sim.runMulti(
        name = name3p,
        rules = rules3p,
        numGames,
        runSeed = makeRunSeed(name3p, salt),
        playerGen = HeuristicPlayer,
        doPrint = false,
        doPrintDetails = false,
        useAnsiColors = false
      )
    }
    return games3p
  }

  def play_game4p(prefix: String, numGames: Int, salt: String): List[fireflower.Game] = {
    val rules4p = Rules.Standard(numPlayers = 4, stopEarlyLoss = false)

    val name4p = prefix + "HeuristicStandard4P"
    val games4p = {
      Sim.runMulti(
        name = name4p,
        rules = rules4p,
        numGames,
        runSeed = makeRunSeed(name4p, salt),
        playerGen = HeuristicPlayer,
        doPrint = false,
        doPrintDetails = false,
        useAnsiColors = false
      )
    }

    return games4p
  }

  def play_game5p(prefix: String, numGames: Int, salt: String): List[fireflower.Game] = {
    val rules5p = Rules.Standard(numPlayers = 5, stopEarlyLoss = false)

    val name5p = prefix + "HeuristicStandard5P"
    val games5p = {
      Sim.runMulti(
        name = name5p,
        rules = rules5p,
        numGames,
        runSeed = makeRunSeed(name5p, salt),
        playerGen = HeuristicPlayer,
        doPrint = true,
        doPrintDetails = false,
        useAnsiColors = false
      )
    }

    return games5p
  }

  def printScoreSummary(rules: Rules, games: List[Game]) = {
    val scoreTable = (0 to rules.maxScore).map { score =>
      (score, games.count(game => game.numPlayed == score))
    }
    val numGames = games.length
    var cumulativeCount = 0
    scoreTable.foreach { case (score, count) =>
      println("Score %2d  Games: %2d  Percent: %4.1f%%  Cum: %4.1f%%".format(
        score, count, count.toDouble * 100.0 / numGames, (numGames - cumulativeCount.toDouble) * 100.0 / numGames
      ))
      cumulativeCount += count
    }
    val avgScore = scoreTable.foldLeft(0) { case (acc, (score, count)) =>
      acc + count * score
    }.toDouble / numGames
    val avgUtility = scoreTable.foldLeft(0) { case (acc, (score, count)) =>
      acc + count * (if (score == rules.maxScore) score * 4 else score * 2)
    }.toDouble / numGames
    println("Average Score: " + avgScore)
    println("Average Utility: " + avgUtility)
  }

  def printScoreSummaryBombZero(rules: Rules, games: List[Game]) = {
    val scoreTable = (0 to rules.maxScore).map { score =>
      (score, games.count(game => (if (game.numBombs > rules.maxBombs) 0 else game.numPlayed) == score))
    }
    val numGames = games.length
    var cumulativeCount = 0
    scoreTable.foreach { case (score, count) =>
      println("Score %2d  Games: %2d  Percent: %4.1f%%  Cum: %4.1f%%".format(
        score, count, count.toDouble * 100.0 / numGames, (numGames - cumulativeCount.toDouble) * 100.0 / numGames
      ))
      cumulativeCount += count
    }
    val avgScore = scoreTable.foldLeft(0) { case (acc, (score, count)) =>
      acc + count * score
    }.toDouble / numGames
    val avgUtility = scoreTable.foldLeft(0) { case (acc, (score, count)) =>
      acc + count * (if (score == rules.maxScore) score * 4 else score * 2)
    }.toDouble / numGames
    println("Average Score: " + avgScore)
    println("Average Utility: " + avgUtility)
  }


  def writeToCsvFile(games: List[fireflower.Game], numPlayer: Int) = {
    var fileName = "fireflower_" + numPlayer.toString + "_" + games.length.toString + ".csv"
    val outputFile = new BufferedWriter(new FileWriter(fileName))
    val csvWriter = new CSVWriter(outputFile)
    var listOfRecords = new ListBuffer[Array[String]]()
    var filledInLine = new ListBuffer[String]()
    var filledInLineFinal = new ListBuffer[String]()

    /*
    Csv file fields:
    - GameNum
    - NumOfCardInDeck
    - Action type                       component(0)
    - Color: 0-4 (-1 if not applied)    component(1)(0)
    - Rank: 0-4 (-1 if not applied)     component(1)(1)
    */

    for (gameNum <- 0 to games.length - 1) {
      var actionsArray = games(gameNum).actions
//      var cardsArray = games(gameNum).playersCard
//      var currPlayerArray = games(gameNum).currPlayerArray
      var initialDeck = games(gameNum).initialDeck

      for (gameStep <- 0 to actionsArray.length - 1) {
        //split actions
        var component = actionsArray(gameStep).split(" ")
        /*
        - component(0): moveType
        - component(1): Card Color/Rank/Position
        */
        if (component(0) != "Bomb") {
          filledInLine = ListBuffer(gameNum.toString, "50", component(0), component(1)(0).toString)

          //fill in the rank
          if (component(0) == "REVEAL_COLOR") {
            filledInLine += "-1"
          }
          else {
            filledInLine += component(1)(1).toString
          }

          //fill in initial deck
          for (i <-0 to initialDeck.length-1) {
            filledInLine += initialDeck(i)
          }

          listOfRecords += filledInLine.toArray
        }

      }

    }

    csvWriter.writeAll(listOfRecords.toList)
    outputFile.close()
  }
}

