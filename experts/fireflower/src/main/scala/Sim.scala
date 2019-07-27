package fireflower

import java.io.{BufferedWriter, FileWriter}

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.util.Random

import au.com.bytecode.opencsv.CSVWriter

object Sim {
  def runSingle(
    rules: Rules,
    gameSeed: Long,
    playerSeed: Long,
    playerGen: PlayerGen,
    doPrint: Boolean,
    useAnsiColors: Boolean,
    debugTurnAndPath: Option[(Int,List[GiveAction])]
  ): Game = {
    val players = playerGen.genPlayers(rules,playerSeed)
    if(players.length != rules.numPlayers)
      throw new Exception("players.length (%d) != rules.numPlayers (%d)".format(players.length,rules.numPlayers))

    if(doPrint)
      println("GameSeed: " + gameSeed + " PlayerSeed: " + playerSeed)

    val game = Game(rules,gameSeed)
    game.drawInitialCards()

    for(pid <- 0 to (players.length - 1)) {
      players(pid).handleGameStart(game.hiddenFor(pid))
    }

    while(!game.isDone()) {
      debugTurnAndPath.foreach { case (turn,path) =>
        if(game.turnNumber == turn)
          game.debugPath = Some(path)
        else
          game.debugPath = None
      }

      val player = players(game.curPlayer)
      val ga = player.getAction(game.hiddenFor(game.curPlayer))
      if(!game.isLegal(ga)) {
        throw new Exception("Illegal action: " + game.giveActionToString(ga))
      }
      else {
        val preGame = Game(game)
        val sa = game.seenAction(ga)
        if(doPrint)
          println(game.toString(useAnsiColors) + "  " + game.seenActionToString(sa,useAnsiColors))
        game.doAction(ga)
        for(pid <- 0 to (players.length - 1)) {
          players(pid).handleSeenAction(sa, preGame.hiddenFor(pid), game.hiddenFor(pid))
        }
      }

      writeToCsvFile()
    }

    if(doPrint) {
      println(game.toString(useAnsiColors))
    }
    game
  }

  def runSingle(
    rules: Rules,
    playerGen: PlayerGen,
    doPrint: Boolean,
    useAnsiColors: Boolean
  ): Game = {
    val rand = Rand()
    val gameSeed = rand.nextLong()
    val playerSeed = rand.nextLong()
    runSingle(
      rules = rules,
      gameSeed = gameSeed,
      playerSeed = playerSeed,
      playerGen = playerGen,
      doPrint = doPrint,
      useAnsiColors = useAnsiColors,
      debugTurnAndPath = None
    )
  }

  def runMulti(
    name: String,
    rules: Rules,
    numGames: Int,
    runSeed: Long,
    playerGen: PlayerGen,
    doPrint: Boolean,
    doPrintDetails: Boolean,
    useAnsiColors: Boolean
  ): List[Game] = {
    if(doPrint)
      println(name + " starting " + numGames + " games, runSeed: " + runSeed)

    val rand = Rand(runSeed)
    val games =
      (0 to (numGames-1)).map { i =>
        val gameSeed = rand.nextLong()
        val playerSeed = rand.nextLong()
        val game = runSingle(
          rules = rules,
          gameSeed = gameSeed,
          playerSeed = playerSeed,
          playerGen = playerGen,
          doPrint = doPrintDetails,
          useAnsiColors = useAnsiColors,
          debugTurnAndPath = None
        )
        if(doPrint)
          println(name + " Game " + i + " Score: " + game.numPlayed + " GameSeed: " + gameSeed)
        game
      }.toList
    games
  }

  def runMulti(
    name: String,
    rules: Rules,
    numGames: Int,
    playerGen: PlayerGen,
    doPrint: Boolean,
    doPrintDetails: Boolean,
    useAnsiColors: Boolean
  ): List[Game] = {
    val rand = Rand()
    runMulti(
      name = name,
      rules = rules,
      numGames = numGames,
      runSeed = rand.nextLong(),
      playerGen = playerGen,
      doPrint = doPrint,
      doPrintDetails = doPrintDetails,
      useAnsiColors = useAnsiColors
    )
  }

  def writeToCsvFile () = {
//    val outputFile = new BufferedWriter(new FileWriter("/Users/phamthanhhuyen/Documents/ganabi/experts/fireflower/data.csv")) //replace the path with the desired path and filename with the desired filename
//    val csvWriter = new CSVWriter(outputFile)
//    val csvFields = Array("name", "age", "major")
//    val employee1 = Array("piyush","23","computerscience")
//    val employee2= Array("neel","24","computerscience")
//    val employee3= Array("aayush","27","computerscience")
//    //val listOfRecords = new ListBuffer[Array[String]]()
//    val listOfRecords = List(csvFields, employee1, employee2, employee3)
//
//    csvWriter.writeAll(listOfRecords) //<-- that listOfRecords right there
//    //Here is the suggestion on how to fix the error but still -.-
//    //https://stackoverflow.com/questions/39217542/type-mismatch-found-java-util-liststringrequired-liststring
//    outputFile.close()

    val outputFile = new BufferedWriter(new FileWriter("/Users/phamthanhhuyen/Documents/ganabi/experts/fireflower/data.csv"))
    val csvWriter = new CSVWriter(outputFile)
    val csvFields = Array("id", "name", "age", "city")
    val nameList = List("Deepak", "Sangeeta", "Geetika", "Anubhav", "Sahil", "Akshay")
    val ageList = (24 to 26).toList
    val cityList = List("Delhi", "Kolkata", "Chennai", "Mumbai")
    val random = new Random()
    var listOfRecords = new ListBuffer[Array[String]]()
    listOfRecords += csvFields
    for (i <- 0 until 10) {
      listOfRecords += Array(i.toString, nameList(random.nextInt(nameList.length))
        , ageList(random.nextInt(ageList.length)).toString, cityList(random.nextInt(cityList.length)))
    }
    csvWriter.writeAll(listOfRecords.toList)
    outputFile.close()
  }
}
