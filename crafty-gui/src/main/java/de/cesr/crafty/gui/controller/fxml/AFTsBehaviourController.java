package de.cesr.crafty.gui.controller.fxml;

import de.cesr.crafty.core.model.Aft;
import de.cesr.crafty.core.utils.general.Utils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;

public class AFTsBehaviourController {
	@FXML
	private Label SNoiseMaxS;
	@FXML
	private Slider GiveInSDS;
	@FXML
	private Slider GiveUpMeanS;
	@FXML
	private Slider GiveUpSDS;
	@FXML
	private Slider SNoiseMinS;
	@FXML
	private Slider ServiceLevelNoiseMaxS;
	@FXML
	private Slider GiveUpProbabiltyS;
	@FXML
	private TextField GiveInMeanT;
	@FXML
	private TextField GiveInSDT;
	@FXML
	private TextField GiveUpMeanT;
	@FXML
	private TextField GiveUpSDT;
	@FXML
	private TextField SNoiseMinT;
	@FXML
	private TextField ServiceLevelNoiseMaxT;
	@FXML
	private TextField GiveUpProbabiltyT;
	@FXML
	private Slider GiveInMeanS;
	@FXML
	private GridPane gridBehevoirButtons;
	
	public void initialize() {
		System.out.println("initialize " + getClass().getSimpleName());


	}
	
	
	static void AgentParametre(Aft agent, GridPane grid) {

		grid.getChildren().clear();

		Slider[] parametrSlider = new Slider[7];
		TextField[] parametrValue = new TextField[7];
		parametrSlider[0] = new Slider(0, 100, agent.getGiveInMean());
		parametrSlider[1] = new Slider(0, 100, agent.getGiveInSD());
		parametrSlider[2] = new Slider(0, 100, agent.getGiveUpMean());
		parametrSlider[3] = new Slider(0, 100, agent.getGiveUpSD());
		parametrSlider[4] = new Slider(0, 1, agent.getServiceLevelNoiseMin());
		parametrSlider[5] = new Slider(0, 1, agent.getServiceLevelNoiseMax());
		parametrSlider[6] = new Slider(0, 1, agent.getGiveUpProbabilty());

		grid.add(new Text(" GiveIn Mean "), 0, 0);
		grid.add(new Text(" GiveIn Standard Deviation"), 0, 1);
		grid.add(new Text(" GiveUp Mean "), 0, 2);
		grid.add(new Text(" GiveUp Standard Deviation"), 0, 3);
		grid.add(new Text(" Service Level Noise Min"), 0, 4);
		grid.add(new Text(" Service Level Noise Max"), 0, 5);
		grid.add(new Text(" GiveUp Probabilty"), 0, 6);
		for (int i = 0; i < parametrValue.length; i++) {
			parametrValue[i] = new TextField(parametrSlider[i].getValue() + "");
			grid.add(parametrSlider[i], 1, i);
			grid.add(parametrValue[i], 2, i);

			int k = i;
			parametrSlider[i].valueProperty().addListener((ov, oldval, newval) -> {
				parametrValue[k].setText("" + parametrSlider[k].getValue());
				switch (k) {
				case 0:
					agent.setGiveInMean(parametrSlider[k].getValue());
					break;
				case 1:
					agent.setGiveInSD(parametrSlider[k].getValue());
					break;
				case 2:
					agent.setGiveUpMean(parametrSlider[k].getValue());
					break;
				case 3:
					agent.setGiveUpSD(parametrSlider[k].getValue());
					break;
				case 4:
					agent.setServiceLevelNoiseMin(parametrSlider[k].getValue());
					break;
				case 5:
					agent.setServiceLevelNoiseMax(parametrSlider[k].getValue());
					break;
				case 6:
					agent.setGiveUpProbabilty(parametrSlider[k].getValue());
					break;
				default:
					break;
				}
			});
			parametrValue[i].setOnKeyPressed(event -> {
				if (event.getCode().equals(KeyCode.ENTER)) {
					parametrSlider[k].setValue(Utils.sToD(parametrValue[k].getText()));
					parametrSlider[k].fireEvent(event);
				}
			});
		}
	}
}
