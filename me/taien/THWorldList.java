package me.taien;

import java.util.Map;

public class THWorldList
{
  public Map common;
  public Map uncommon;
  public Map rare;
  public Map legendary;
  public Map epic;

  public THWorldList(Map common, Map uncommon, Map rare, Map legendary, Map epic)
  {
    this.common = common;
    this.uncommon = uncommon;
    this.rare = rare;
    this.legendary = legendary;
    this.epic = epic;
  }
}