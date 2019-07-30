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
        100
    }
    val numPlayers = {
      if(args.length >= 2)
        args(1).toInt
      else
        3
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
    writeToCsvFile(base_dir = "/Users/phamthanhhuyen/Documents", games, numPlayers)

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
        doPrintDetails = false,
        useAnsiColors = false
      )
    }

    //val allGamesSteps: ListBuffer[Int] = getGameStepsForEachGame(games2p)

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
        doPrintDetails = true,
        useAnsiColors = false
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
        useAnsiColors = false
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

  def getCsvFields (numPlayer: Int): Array[String] = {
    val csvFields = ListBuffer("Game Number", "Game Step", "Observations", "Move Type", "Card Color/Rank/Position", "Cards Involving")
    for (i <- 0 to numPlayer-1) {
      csvFields += "Player " + (i+1).toString
    }

    return csvFields.toArray
  }

  def writeToCsvFile (base_dir: String, games: List[fireflower.Game], numPlayer: Int) = {
    val outputFile = new BufferedWriter(new FileWriter(base_dir + "/ganabi/experts/fireflower/data.csv"))
    val csvWriter = new CSVWriter(outputFile)
    val csvFields = getCsvFields(numPlayer)
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
      var cardsArray = games(gameNum).playersCard
      for (gameStep <- 0 to actionsArray.length-1) {
        //split actions
        var component = actionsArray(gameStep).split(" ")
        //listOfRecords += Array((gameNum+1).toString, (gameStep+1).toString, " ", component(0), component(1), component(2))
        //get each players card
        var filledInLine = ListBuffer((gameNum+1).toString, (gameStep+1).toString, " ", component(0), component(1), component(2))
        var playerCard = cardsArray(gameStep).split('|')
        for (player <- 0 to playerCard.length-1) {
          filledInLine += playerCard(player)
        }

        listOfRecords += filledInLine.toArray
      }
    }

    csvWriter.writeAll(listOfRecords.toList)
    outputFile.close()
  }




  /*

*/

}
