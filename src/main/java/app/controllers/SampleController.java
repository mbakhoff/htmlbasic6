package app.controllers;

import app.model.SampleItem;
import app.services.SampleItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@Transactional
public class SampleController {

  private final SampleItemRepository items;

  @Autowired
  public SampleController(SampleItemRepository items) {
    this.items = items;
  }

  @RequestMapping(value = "/samples")
  public String showAllItems(Model model) {
    model.addAttribute("items", items.findAll());
    return "samples_index";
  }

  @RequestMapping(value = "/samples", method = RequestMethod.POST)
  public String handleNewItemForm(SampleItem item) {
    items.save(item);
    return "redirect:/samples";
  }

  @RequestMapping(value = "/samples/new")
  public String showNewItemForm(Model model) {
    model.addAttribute("sampleItem", new SampleItem());
    return "samples_new";
  }

  @RequestMapping(value = "/samples/{id}")
  public String showSingleItemForm(@PathVariable Long id, Model model) {
    model.addAttribute("sampleItem", items.findOne(id));
    return "samples_view";
  }

  @RequestMapping(value = "/samples/{id}", method = RequestMethod.POST, params = {"save"})
  public String handleItemUpdate(SampleItem item, @PathVariable Long id) {
    items.save(item);
    return "samples_view";
  }

  @RequestMapping(value = "/samples/{id}", method = RequestMethod.POST, params = {"delete"})
  public String handleItemDelete(SampleItem item, @PathVariable Long id) {
    items.delete(id);
    return "redirect:/samples";
  }

  @RequestMapping(value = "/samples/{id}/edit")
  public String showSingleItemEditForm(@PathVariable Long id, Model model) {
    model.addAttribute("sampleItem", items.findOne(id));
    return "samples_edit";
  }
}
