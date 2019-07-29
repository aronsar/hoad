package fireflower
import java.io.{BufferedWriter, FileWriter}
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import au.com.bytecode.opencsv.CSVWriter

object PlayerTests {

  def main(args: Array[String]): Unit = {
    val numGames = {
      if(args.length >= 1)
        args(0).toInt
      else
        1
    }
    val numPlayers = {
      if(args.length >= 2)
        args(1).toInt
      else
        2
    }
    println("NumGames=" + numGames)

    runTests(prefix="",salt="g",numGames=numGames, numPlayers=numPlayers)
  }

  def makeRunSeed(name:String, salt:String): Long = {
    RandUtils.sha256Long(RandUtils.sha256Long(name) + salt)
  }

  def runTests(prefix: String, salt: String, numGames: Int, numPlayers: Int): Unit = {
    val start = System.nanoTime()
    var games: List[fireflower.Game] = List[fireflower.Game]()

    if (numPlayers==3) {
      games = play_game3p(prefix, numGames, salt)
    } else if (numPlayers == 4) {
      games = play_game4p(prefix, numGames, salt)
    } else {
      games = play_game2p(prefix, numGames, salt)
    }

    val gameStepArray: ListBuffer[Int] = getGameStepsForEachGame(games)
    writeToCsvFile(base_dir = "/Users/phamthanhhuyen/Documents", games)

    val end = System.nanoTime()

    println("Done!")
    println("")
    println("Time: " + (end-start).toDouble / 1.0e9)
  }

  def getGameStepsForEachGame(games: List[fireflower.Game]): ListBuffer[Int]={
    var gameStepArray = new ListBuffer[Int]()

    for (i <- 0 to games.length-1) {
      gameStepArray += games(i).turnNumber
    }
    return gameStepArray
  }


  def play_game2p(prefix: String, numGames: Int, salt: String): List[fireflower.Game]={
    val rules2p = Rules.Standard(numPlayers=2,stopEarlyLoss=false)
    val name2p = prefix + "HeuristicStandard2P"
    val games2p = {
      Sim.runMulti(
        name = name2p,
        rules = rules2p,
        numGames,
        runSeed = makeRunSeed(name2p, salt),
        playerGen = HeuristicPlayer,
        doPrint = true,
        doPrintDetails = true,
        useAnsiColors = false
      )
    }

    //val allGamesSteps: ListBuffer[Int] = getGameStepsForEachGame(games2p)

    println(name2p + ":")
    //    printScoreSummary(rules2p,games2p)
    //
    //    println("")
    //    println(name2p + ":")
    //    printScoreSummary(rules2p,games2p)
    //    printScoreSummaryBombZero(rules2p,games2p)

    //a list of numGames of 2p games
    return games2p
  }

  def play_game3p(prefix: String, numGames: Int, salt: String): List[fireflower.Game]={
    val rules3p = Rules.Standard(numPlayers=3,stopEarlyLoss=false)

    val name3p = prefix + "HeuristicStandard3P"
    val games3p = {
      Sim.runMulti(
        name = name3p,
        rules = rules3p,
        numGames,
        runSeed = makeRunSeed(name3p, salt),
        playerGen = HeuristicPlayer,
        doPrint = true,
        doPrintDetails = false,
        useAnsiColors = true
      )
    }

    return games3p
  }

  def play_game4p(prefix: String, numGames: Int, salt: String): List[fireflower.Game]={
    val rules4p = Rules.Standard(numPlayers=4,stopEarlyLoss=false)

    val name4p = prefix + "HeuristicStandard4P"
    val games4p = {
      Sim.runMulti(
        name = name4p,
        rules = rules4p,
        numGames,
        runSeed = makeRunSeed(name4p, salt),
        playerGen = HeuristicPlayer,
        doPrint = true,
        doPrintDetails = false,
        useAnsiColors = true
      )
    }

    return games4p
  }

  def printScoreSummary(rules: Rules, games: List[Game]) = {
    val scoreTable = (0 to rules.maxScore).map { score =>
      (score,games.count(game => game.numPlayed == score))
    }
    val numGames = games.length
    var cumulativeCount = 0
    scoreTable.foreach { case (score,count) =>
      println("Score %2d  Games: %2d  Percent: %4.1f%%  Cum: %4.1f%%".format(
        score, count, count.toDouble * 100.0 / numGames, (numGames - cumulativeCount.toDouble) * 100.0 / numGames
      ))
      cumulativeCount += count
    }
    val avgScore = scoreTable.foldLeft(0) { case (acc,(score,count)) =>
      acc + count * score
    }.toDouble / numGames
    val avgUtility = scoreTable.foldLeft(0) { case (acc,(score,count)) =>
      acc + count * (if(score == rules.maxScore) score * 4 else score * 2)
    }.toDouble / numGames
    println("Average Score: " + avgScore)
    println("Average Utility: " + avgUtility)
  }

  def printScoreSummaryBombZero(rules: Rules, games: List[Game]) = {
    val scoreTable = (0 to rules.maxScore).map { score =>
      (score,games.count(game => (if (game.numBombs > rules.maxBombs) 0 else game.numPlayed) == score))
    }
    val numGames = games.length
    var cumulativeCount = 0
    scoreTable.foreach { case (score,count) =>
      println("Score %2d  Games: %2d  Percent: %4.1f%%  Cum: %4.1f%%".format(
        score, count, count.toDouble * 100.0 / numGames, (numGames - cumulativeCount.toDouble) * 100.0 / numGames
      ))
      cumulativeCount += count
    }
    val avgScore = scoreTable.foldLeft(0) { case (acc,(score,count)) =>
      acc + count * score
    }.toDouble / numGames
    val avgUtility = scoreTable.foldLeft(0) { case (acc,(score,count)) =>
      acc + count * (if(score == rules.maxScore) score * 4 else score * 2)
    }.toDouble / numGames
    println("Average Score: " + avgScore)
    println("Average Utility: " + avgUtility)
  }

  def writeToCsvFile (base_dir: String, games: List[fireflower.Game]) = {
    val outputFile = new BufferedWriter(new FileWriter(base_dir + "/ganabi/experts/fireflower/data.csv"))
    val csvWriter = new CSVWriter(outputFile)
    val csvFields = Array("Game Number", "Game Step", "Observations", "Move Type", "Card Color/Rank/Position", "Cards Involving")
    var listOfRecords = new ListBuffer[Array[String]]()

    //Card Rank is marked on based 0 (card Red 1-5 will be R0-4 etc
    listOfRecords += csvFields
    /*
    Each action has 3 components:
    - Move type: HintColor, HintRank, Play, Discard or Bomb
    - Depending on the move type, the 2nd component has 3 options:
      + If type==HintColor, it will be either RYWBG
      + If type==HintRank, it will be 0-4, representing card number from 1-5
      + If type==Play or Discard or Bomb, it will be 0-4, representing the position of the card on hand
    - Card Involving: which card(s) that the action is related to
    */

    for (gameNum <- 0 to games.length-1) {
      var actionsArray = games(gameNum).actions
      for (gameStep <- 0 to actionsArray.length-1) {
        var component = actionsArray(gameStep).split(" ")
        listOfRecords += Array((gameNum+1).toString, (gameStep+1).toString, " ", component(0), component(1), component(2))
      }
    }

    csvWriter.writeAll(listOfRecords.toList)
    outputFile.close()
  }




  /*
   Results:

   [info] HeuristicStandard2P:
   [info] Score  0  Games:  0  Percent:  0.0%  Cum: 100.0%
   [info] Score  1  Games:  0  Percent:  0.0%  Cum: 100.0%
   [info] Score  2  Games:  1  Percent:  0.1%  Cum: 100.0%
   [info] Score  3  Games:  0  Percent:  0.0%  Cum: 99.9%
   [info] Score  4  Games:  0  Percent:  0.0%  Cum: 99.9%
   [info] Score  5  Games:  0  Percent:  0.0%  Cum: 99.9%
   [info] Score  6  Games:  1  Percent:  0.1%  Cum: 99.9%
   [info] Score  7  Games:  0  Percent:  0.0%  Cum: 99.8%
   [info] Score  8  Games:  1  Percent:  0.1%  Cum: 99.8%
   [info] Score  9  Games:  2  Percent:  0.2%  Cum: 99.7%
   [info] Score 10  Games:  2  Percent:  0.2%  Cum: 99.5%
   [info] Score 11  Games:  3  Percent:  0.3%  Cum: 99.3%
   [info] Score 12  Games:  1  Percent:  0.1%  Cum: 99.0%
   [info] Score 13  Games:  1  Percent:  0.1%  Cum: 98.9%
   [info] Score 14  Games:  4  Percent:  0.4%  Cum: 98.8%
   [info] Score 15  Games:  3  Percent:  0.3%  Cum: 98.4%
   [info] Score 16  Games:  7  Percent:  0.7%  Cum: 98.1%
   [info] Score 17  Games:  9  Percent:  0.9%  Cum: 97.4%
   [info] Score 18  Games: 15  Percent:  1.5%  Cum: 96.5%
   [info] Score 19  Games: 28  Percent:  2.8%  Cum: 95.0%
   [info] Score 20  Games: 23  Percent:  2.3%  Cum: 92.2%
   [info] Score 21  Games: 38  Percent:  3.8%  Cum: 89.9%
   [info] Score 22  Games: 58  Percent:  5.8%  Cum: 86.1%
   [info] Score 23  Games: 114  Percent: 11.4%  Cum: 80.3%
   [info] Score 24  Games: 124  Percent: 12.4%  Cum: 68.9%
   [info] Score 25  Games: 565  Percent: 56.5%  Cum: 56.5%
   [info] Average Score: 23.537
   [info] Average Utility: 75.324
*/

}
