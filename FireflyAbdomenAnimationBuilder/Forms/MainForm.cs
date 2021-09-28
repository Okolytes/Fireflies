using System;
using System.Drawing;
using System.IO;
using System.Reflection;
using System.Threading.Tasks;
using System.Web.Script.Serialization;
using System.Windows.Forms;

namespace FireflyAbdomenAnimationBuilder.Forms
{
    public partial class MainForm : Form
    {
        private readonly JavaScriptSerializer _serializer = new JavaScriptSerializer();
        private string _openedFile;

        private Animation _simAnimation;
        private int _simFrame;
        private int _simDelay;
        private float _simGlow;

        [STAThread]
        private static void Main()
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new MainForm());
        }

        private MainForm()
        {
            this.InitializeComponent();

            // Enable double buffering on panels, reducing the flicker
            typeof(Panel).InvokeMember("DoubleBuffered",
                BindingFlags.SetProperty | BindingFlags.Instance | BindingFlags.NonPublic,
                null, this.panelSimulation, new object[] { true });
        }

        private void timerSimulation_Tick(object sender, EventArgs e)
        {
            if (this._simAnimation == null)
                return;

            if (this._simDelay > 0)
            {
                this._simDelay--;
                this.lblSimDelay.Text = $"Delay: {this._simDelay}";
                return;
            }

            int frames = this._simAnimation.Frames.Length - 1;
            if (this._simFrame > frames)
            {
                this._simFrame = 0;
            }

            this._simGlow = this._simAnimation.Frames[this._simFrame];
            this.panelSimulation.BackColor = Color.FromArgb((int)(this._simGlow * 255f), 255, 222, 39);
            this.lblSimGlow.Text = $"Glow: {this._simGlow}";
            this.lblSimFrame.Text = $"Frame: {this._simFrame} / {frames}";
            this.tbSimFrames.Maximum = frames;
            this.tbSimFrames.Value = this._simFrame;

            foreach (Animation.Delay delay in this._simAnimation.Delays)
            {
                if (delay.Frame == this._simFrame)
                {
                    this._simDelay = new Random().Next(delay.Min, delay.Max);
                }
            }

            this._simFrame++;
        }

        private void chkSimToggle_CheckedChanged(object sender, EventArgs e)
        {
            this.chkSimToggle.Text = this.chkSimToggle.Checked ? "Pause" : "Play";
            this.timerSimulation.Enabled = this.chkSimToggle.Checked;
        }

        private void btnGenerateGradient_Click(object sender, EventArgs e)
        {
            using (var gradientForm = new GradientForm())
            {
                gradientForm.ShowDialog(this);
            }
        }

        private void btnOpenFile_Click(object sender, EventArgs e)
        {
            using (OpenFileDialog fileDialog = this.openFileDialog1)
            {
                if (fileDialog.ShowDialog() == DialogResult.OK)
                {
                    this._openedFile = fileDialog.FileName;
                    this.DeserializeFromOpenedFile();
                }
            }
        }

        private void DeserializeFromOpenedFile()
        {
            if (this._openedFile != null)
            {
                if (!File.Exists(this._openedFile))
                {
                    MessageBox.Show("Couldn't refresh opened file, may have been renamed or moved");
                    this._openedFile = null;
                    return;
                }
                this._simAnimation = this._serializer.Deserialize<Animation>(File.ReadAllText(this._openedFile));
            }
        }

        private void btnRefresh_Click(object sender, EventArgs e)
        {
            this.DeserializeFromOpenedFile();
        }
    }
}