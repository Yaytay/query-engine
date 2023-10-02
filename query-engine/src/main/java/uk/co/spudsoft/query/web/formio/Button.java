/*
 * Copyright (C) 2023 njt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.co.spudsoft.query.web.formio;

/**
 *
 * @author njt
 */
public class Button extends Component<Button> {

  public static enum ActionType {
    submit, reset, event, oauth
  }
  
  private String size;
  private String leftIcon;
  private String rightIcon;
  private Boolean block;
  private ActionType action;
  private Boolean disableOnInvalid;
  private String theme;
  
  public Button() {
    super("button");
  }

  public String getSize() {
    return size;
  }

  public void setSize(String size) {
    this.size = size;
  }

  public String getLeftIcon() {
    return leftIcon;
  }

  public void setLeftIcon(String leftIcon) {
    this.leftIcon = leftIcon;
  }

  public String getRightIcon() {
    return rightIcon;
  }

  public void setRightIcon(String rightIcon) {
    this.rightIcon = rightIcon;
  }

  public Boolean getBlock() {
    return block;
  }

  public void setBlock(Boolean block) {
    this.block = block;
  }

  public ActionType getAction() {
    return action;
  }

  public void setAction(ActionType action) {
    this.action = action;
  }

  public Boolean getDisableOnInvalid() {
    return disableOnInvalid;
  }

  public void setDisableOnInvalid(Boolean disableOnInvalid) {
    this.disableOnInvalid = disableOnInvalid;
  }

  public String getTheme() {
    return theme;
  }

  public void setTheme(String theme) {
    this.theme = theme;
  }

  public Button withSize(final String value) {
    this.size = value;
    return this;
  }

  public Button withLeftIcon(final String value) {
    this.leftIcon = value;
    return this;
  }

  public Button withRightIcon(final String value) {
    this.rightIcon = value;
    return this;
  }

  public Button withBlock(final Boolean value) {
    this.block = value;
    return this;
  }

  public Button withAction(final ActionType value) {
    this.action = value;
    return this;
  }

  public Button withDisableOnInvalid(final Boolean value) {
    this.disableOnInvalid = value;
    return this;
  }

  public Button withTheme(final String value) {
    this.theme = value;
    return this;
  }
}
