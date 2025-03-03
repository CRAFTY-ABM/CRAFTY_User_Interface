package de.cesr.crafty.core.cli;

public class CraftyOptions {
	private String yamlConfigFilePath;
	private String project_path;
	private String scenario_Name;
	

	public String getConfigFilePath() {
		return yamlConfigFilePath;
	}

	public void setConfigFilePath(String configFilePath) {
		this.yamlConfigFilePath = configFilePath;
	}

	public String getProjectDirectoryPath() {
		return project_path;
	}

	public void setProjectDirectoryPath(String projectDirectoryPath) {
		this.project_path = projectDirectoryPath;
	}

	public String getScenario_Name() {
		return scenario_Name;
	}

	public void setScenario_Name(String scenario_Name) {
		this.scenario_Name = scenario_Name;
	}
	
}
