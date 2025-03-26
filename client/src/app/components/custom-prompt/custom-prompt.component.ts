import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-custom-prompt',
  standalone: false,
  templateUrl: './custom-prompt.component.html',
  styleUrl: './custom-prompt.component.css'
})
export class CustomPromptComponent {
  @Input() activeCustomPrompt: string | null = null;
  @Input() isApplyingPrompt = false;
  @Output() applyPrompt = new EventEmitter<string>();
  @Output() resetPromptEvent = new EventEmitter<void>();

  customPrompt: string = '';

  submitPrompt(): void {
    if (!this.customPrompt || this.customPrompt.trim() === '') {
      return;
    }
    this.applyPrompt.emit(this.customPrompt.trim());
    this.customPrompt = '';
  }

  resetPrompt(): void {
    this.resetPromptEvent.emit();
  }
}