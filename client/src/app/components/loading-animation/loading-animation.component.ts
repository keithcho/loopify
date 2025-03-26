import { Component, OnInit, OnDestroy } from '@angular/core';

@Component({
  selector: 'app-loading-animation',
  standalone: false,
  templateUrl: './loading-animation.component.html',
  styleUrls: ['./loading-animation.component.css']
})
export class LoadingAnimationComponent implements OnInit, OnDestroy {
  animatingBars: boolean[] = Array(16).fill(false);
  tips: string[] = [
    "🎶 Discover new artists similar to your favorites",
    "🎶 Find hidden gems from genres you already love",
    "🎶 Expand your musical horizons with our recommendations",
    "🎶 Creating personalized suggestions based on your taste",
    "🎶 Looking for songs that match your vibe...",
    "🎶 Analyzing your musical preferences",
    "🎶 Finding the perfect soundtrack for your day"
  ];
  currentTipIndex = 0;
  
  private barInterval: any;
  private tipInterval: any;

  ngOnInit(): void {
    // Handle random bar animations
    this.barInterval = setInterval(() => {
      this.triggerRandomBar();
    }, 200);

    // Rotate through tips
    this.tipInterval = setInterval(() => {
      this.currentTipIndex = (this.currentTipIndex + 1) % this.tips.length;
    }, 4000);
  }

  ngOnDestroy(): void {
    // Clear intervals when component is destroyed
    if (this.barInterval) {
      clearInterval(this.barInterval);
    }
    if (this.tipInterval) {
      clearInterval(this.tipInterval);
    }
  }

  // Get random bar height for wave effect
  getRandomBarHeight(): number {
    return Math.floor(Math.random() * 24) + 10; // Between 10px and 34px
  }

  // Function to trigger a random bar to animate
  triggerRandomBar(): void {
    const index = Math.floor(Math.random() * this.animatingBars.length);
    const newAnimatingBars = [...this.animatingBars];
    newAnimatingBars[index] = true;
    this.animatingBars = newAnimatingBars;

    // Reset after 500ms
    setTimeout(() => {
      const resetBars = [...this.animatingBars];
      resetBars[index] = false;
      this.animatingBars = resetBars;
    }, 500);
  }
}