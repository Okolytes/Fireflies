using System;
using System.Collections.Generic;
using System.Windows.Forms;

namespace FireflyAbdomenAnimationBuilder.Forms
{
    public partial class GradientForm : Form
    {
        private readonly List<float> _frames = new List<float>();

        public GradientForm()
        {
            this.InitializeComponent();
            this.DialogResult = DialogResult.Cancel;
        }

        private void NumValueChanged()
        {
            var steps = (float)this.numStep.Value;
            if (this.numStep.Value <= 0) return;

            var from = (float)this.numFrom.Value;
            var to = (float)this.numTo.Value;
            bool reverse = from > to;
            if (reverse) steps = -steps;

            this._frames.Clear();
            for (float i = from; reverse ? i >= to : i < to; i += steps)
            {
                this._frames.Add((float)Math.Round(i, 2));
            }

            this.lblFrames.Text = $"{this._frames.Count} Frames";

            this.listBox1.Items.Clear();
            this._frames.ForEach(f => this.listBox1.Items.Add(f));
        }

        private void btnOk_Click(object sender, EventArgs e)
        {
            if (this._frames.Count > 0)
            {
                var s = string.Empty;
                this._frames.ForEach(f => s += $"{f},\n");
                Clipboard.SetText(s);
            }
            this.Close();
        }

        private void btnCancel_Click(object sender, EventArgs e)
        {
            this.Close();
        }

        private void numFrom_ValueChanged(object sender, EventArgs e)
        {
            this.NumValueChanged();
        }

        private void numTo_ValueChanged(object sender, EventArgs e)
        {
            this.NumValueChanged();
        }

        private void numStep_ValueChanged(object sender, EventArgs e)
        {
            this.NumValueChanged();
        }
    }
}