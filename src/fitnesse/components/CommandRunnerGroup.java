// Copyright (C) 2003,2004,2005 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the GNU General Public License version 2 or later.

package fitnesse.components;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CommandRunnerGroup {
  private Map<String, CommandRunner> commandRunners;

  public CommandRunnerGroup() {
    commandRunners = new HashMap<String, CommandRunner>();
  }

  public CommandRunnerGroup(CommandRunner commandRunner) {
    this();
    addCommandRunner("default", commandRunner);
  }

  public void addCommandRunner(String portType, CommandRunner commandRunner) {
    commandRunners.put(portType.toUpperCase(), commandRunner);
  }

  public CommandRunner getCommandRunner(String portType) {
    return commandRunners.get(portType.toUpperCase());
  }

  public void asynchronousStart() throws Exception {
    for (CommandRunner commandRunner : commandRunners.values()) {
      if (!commandRunner.isStarted())
        commandRunner.asynchronousStart();
    }
  }

  public void run() throws Exception {
    for (CommandRunner commandRunner : commandRunners.values()) {
      commandRunner.run();
    }
  }

  public void join() throws Exception {
    for (CommandRunner commandRunner : commandRunners.values()) {
      commandRunner.join();
    }
  }

  public void kill() throws Exception {
    for (CommandRunner commandRunner : commandRunners.values()) {
      commandRunner.kill();
    }
  }

  public String getFirstCommandLine() {
    if (commandRunners.values().size() > 0) {
      return ((CommandRunner) commandRunners.values().toArray()[0]).getCommand();
    }
    return "";
  }

  public Map<String, CommandRunner> getCommandRunners() {
    return commandRunners;
  }

  public CommandRunner getFirstCommandRunner() {
    if (commandRunners.values().size() > 0) {
      return (CommandRunner) commandRunners.values().toArray()[0];
    }

    return null;
  }

  public boolean wroteToErrorStream() {
    boolean wroteToStream = true;
    for (CommandRunner commandRunner : commandRunners.values()) {
      wroteToStream &= commandRunner.wroteToErrorStream();
    }
    return wroteToStream;
  }

  public boolean wroteToOutputStream() {
    boolean wroteToStream = true;
    for (CommandRunner commandRunner : commandRunners.values()) {
      wroteToStream &= commandRunner.wroteToOutputStream();
    }
    return wroteToStream;
  }

}