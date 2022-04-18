-- MySQL Script generated by MySQL Workbench
-- Mon Apr 18 22:45:47 2022
-- Model: New Model    Version: 1.0
-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema TeachingTutorials
-- -----------------------------------------------------
DROP SCHEMA IF EXISTS `TeachingTutorials` ;

-- -----------------------------------------------------
-- Schema TeachingTutorials
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `TeachingTutorials` DEFAULT CHARACTER SET utf8 ;
USE `TeachingTutorials` ;

-- -----------------------------------------------------
-- Table `TeachingTutorials`.`Players`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `TeachingTutorials`.`Players` ;

CREATE TABLE IF NOT EXISTS `TeachingTutorials`.`Players` (
  `UUID` CHAR(36) NOT NULL,
  `CompletedCompulsory` TINYINT NOT NULL DEFAULT 0,
  `InLesson` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`UUID`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `TeachingTutorials`.`Locations`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `TeachingTutorials`.`Locations` ;

CREATE TABLE IF NOT EXISTS `TeachingTutorials`.`Locations` (
  `LocationID` INT NOT NULL AUTO_INCREMENT,
  `Difficulty` FLOAT NOT NULL,
  PRIMARY KEY (`LocationID`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `TeachingTutorials`.`Lessons`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `TeachingTutorials`.`Lessons` ;

CREATE TABLE IF NOT EXISTS `TeachingTutorials`.`Lessons` (
  `LessonID` INT NOT NULL AUTO_INCREMENT,
  `UUID` CHAR(36) NOT NULL,
  `Finished` TINYINT NOT NULL,
  `StageAt` INT NOT NULL,
  `StepAt` INT NOT NULL,
  `LocationID` INT NOT NULL,
  PRIMARY KEY (`LessonID`),
  INDEX `User_idx` (`UUID` ASC) VISIBLE,
  INDEX `Location_idx` (`LocationID` ASC) VISIBLE,
  CONSTRAINT `User`
    FOREIGN KEY (`UUID`)
    REFERENCES `TeachingTutorials`.`Players` (`UUID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `Location`
    FOREIGN KEY (`LocationID`)
    REFERENCES `TeachingTutorials`.`Locations` (`LocationID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `TeachingTutorials`.`Tutorials`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `TeachingTutorials`.`Tutorials` ;

CREATE TABLE IF NOT EXISTS `TeachingTutorials`.`Tutorials` (
  `TutorialID` INT NOT NULL,
  `InUse` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`TutorialID`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `TeachingTutorials`.`CategoryPoints`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `TeachingTutorials`.`CategoryPoints` ;

CREATE TABLE IF NOT EXISTS `TeachingTutorials`.`CategoryPoints` (
  `TutorialID` INT NOT NULL,
  `Category` ENUM('tpll', 'worldedit', 'colouring', 'detail', 'terraforming') NOT NULL,
  `Difficulty` FLOAT NOT NULL,
  PRIMARY KEY (`TutorialID`, `Category`, `Difficulty`),
  CONSTRAINT `Tutorial`
    FOREIGN KEY (`TutorialID`)
    REFERENCES `TeachingTutorials`.`Tutorials` (`TutorialID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `TeachingTutorials`.`Stages`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `TeachingTutorials`.`Stages` ;

CREATE TABLE IF NOT EXISTS `TeachingTutorials`.`Stages` (
  `StageID` INT NOT NULL,
  `TutorialID` INT NOT NULL,
  `Order` INT NOT NULL,
  PRIMARY KEY (`StageID`),
  INDEX `Tutorial_idx` (`TutorialID` ASC) VISIBLE,
  CONSTRAINT `Tutorial`
    FOREIGN KEY (`TutorialID`)
    REFERENCES `TeachingTutorials`.`Tutorials` (`TutorialID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `TeachingTutorials`.`Steps`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `TeachingTutorials`.`Steps` ;

CREATE TABLE IF NOT EXISTS `TeachingTutorials`.`Steps` (
  `StepID` INT NOT NULL AUTO_INCREMENT,
  `StageID` INT NOT NULL,
  `StepInStage` INT NOT NULL,
  PRIMARY KEY (`StepID`),
  INDEX `Stage_idx` (`StageID` ASC) VISIBLE,
  CONSTRAINT `Stage`
    FOREIGN KEY (`StageID`)
    REFERENCES `TeachingTutorials`.`Stages` (`StageID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `TeachingTutorials`.`Groups`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `TeachingTutorials`.`Groups` ;

CREATE TABLE IF NOT EXISTS `TeachingTutorials`.`Groups` (
  `GroupID` INT NOT NULL,
  `StepID` INT NOT NULL,
  `TpllDifficulty` FLOAT NOT NULL DEFAULT 0,
  `WEDifficulty` FLOAT NOT NULL,
  `TerraDifficulty` FLOAT NOT NULL,
  `ColouringDifficulty` FLOAT NOT NULL,
  `TexturingDifficulty` FLOAT NOT NULL,
  PRIMARY KEY (`GroupID`),
  INDEX `StepID_idx` (`StepID` ASC) VISIBLE,
  CONSTRAINT `StepID`
    FOREIGN KEY (`StepID`)
    REFERENCES `TeachingTutorials`.`Steps` (`StepID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `TeachingTutorials`.`Tasks`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `TeachingTutorials`.`Tasks` ;

CREATE TABLE IF NOT EXISTS `TeachingTutorials`.`Tasks` (
  `TaskID` INT NOT NULL,
  `GroupID` INT NOT NULL,
  `TaskType` VARCHAR(32) NULL,
  `Order` INT NOT NULL,
  PRIMARY KEY (`TaskID`),
  INDEX `Group_idx` (`GroupID` ASC) VISIBLE,
  CONSTRAINT `Group`
    FOREIGN KEY (`GroupID`)
    REFERENCES `TeachingTutorials`.`Groups` (`GroupID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `TeachingTutorials`.`Scores`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `TeachingTutorials`.`Scores` ;

CREATE TABLE IF NOT EXISTS `TeachingTutorials`.`Scores` (
  `LessonID` INT NOT NULL,
  `Category` VARCHAR(45) NOT NULL,
  `Score` INT NOT NULL,
  PRIMARY KEY (`LessonID`),
  CONSTRAINT `LessonID`
    FOREIGN KEY (`LessonID`)
    REFERENCES `TeachingTutorials`.`Lessons` (`LessonID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `TeachingTutorials`.`LocationTasks`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `TeachingTutorials`.`LocationTasks` ;

CREATE TABLE IF NOT EXISTS `TeachingTutorials`.`LocationTasks` (
  `LocationID` INT NOT NULL,
  `TaskID` INT NOT NULL,
  `Answers` MEDIUMTEXT NULL,
  `Difficulty` FLOAT NULL,
  INDEX `Location_idx` (`LocationID` ASC) VISIBLE,
  INDEX `Task_idx` (`TaskID` ASC) VISIBLE,
  CONSTRAINT `Location0`
    FOREIGN KEY (`LocationID`)
    REFERENCES `TeachingTutorials`.`Locations` (`LocationID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `Task`
    FOREIGN KEY (`TaskID`)
    REFERENCES `TeachingTutorials`.`Tasks` (`TaskID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
