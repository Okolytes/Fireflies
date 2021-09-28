
namespace FireflyAbdomenAnimationBuilder.Forms
{
    partial class MainForm
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            this.components = new System.ComponentModel.Container();
            this.panelSimulation = new System.Windows.Forms.Panel();
            this.timerSimulation = new System.Windows.Forms.Timer(this.components);
            this.panel2 = new System.Windows.Forms.Panel();
            this.groupBox1 = new System.Windows.Forms.GroupBox();
            this.lblSimGlow = new System.Windows.Forms.Label();
            this.chkSimToggle = new System.Windows.Forms.CheckBox();
            this.lblSimDelay = new System.Windows.Forms.Label();
            this.lblSimFrame = new System.Windows.Forms.Label();
            this.tbSimFrames = new System.Windows.Forms.TrackBar();
            this.btnAddSlope = new System.Windows.Forms.Button();
            this.btnOpenFile = new System.Windows.Forms.Button();
            this.btnRefresh = new System.Windows.Forms.Button();
            this.openFileDialog1 = new System.Windows.Forms.OpenFileDialog();
            this.panel2.SuspendLayout();
            this.groupBox1.SuspendLayout();
            ((System.ComponentModel.ISupportInitialize)(this.tbSimFrames)).BeginInit();
            this.SuspendLayout();
            // 
            // panelSimulation
            // 
            this.panelSimulation.Dock = System.Windows.Forms.DockStyle.Fill;
            this.panelSimulation.Location = new System.Drawing.Point(0, 0);
            this.panelSimulation.Name = "panelSimulation";
            this.panelSimulation.Size = new System.Drawing.Size(133, 133);
            this.panelSimulation.TabIndex = 0;
            // 
            // timerSimulation
            // 
            this.timerSimulation.Enabled = true;
            this.timerSimulation.Interval = 50;
            this.timerSimulation.Tick += new System.EventHandler(this.timerSimulation_Tick);
            // 
            // panel2
            // 
            this.panel2.BackColor = System.Drawing.Color.FromArgb(((int)(((byte)(56)))), ((int)(((byte)(44)))), ((int)(((byte)(47)))));
            this.panel2.BorderStyle = System.Windows.Forms.BorderStyle.FixedSingle;
            this.panel2.Controls.Add(this.panelSimulation);
            this.panel2.Location = new System.Drawing.Point(6, 19);
            this.panel2.Name = "panel2";
            this.panel2.Size = new System.Drawing.Size(135, 135);
            this.panel2.TabIndex = 2;
            // 
            // groupBox1
            // 
            this.groupBox1.Controls.Add(this.lblSimGlow);
            this.groupBox1.Controls.Add(this.chkSimToggle);
            this.groupBox1.Controls.Add(this.lblSimDelay);
            this.groupBox1.Controls.Add(this.lblSimFrame);
            this.groupBox1.Controls.Add(this.tbSimFrames);
            this.groupBox1.Controls.Add(this.panel2);
            this.groupBox1.Location = new System.Drawing.Point(151, 12);
            this.groupBox1.Name = "groupBox1";
            this.groupBox1.Size = new System.Drawing.Size(147, 322);
            this.groupBox1.TabIndex = 3;
            this.groupBox1.TabStop = false;
            this.groupBox1.Text = "Simulation";
            // 
            // lblSimGlow
            // 
            this.lblSimGlow.AutoSize = true;
            this.lblSimGlow.Location = new System.Drawing.Point(6, 157);
            this.lblSimGlow.Name = "lblSimGlow";
            this.lblSimGlow.Size = new System.Drawing.Size(43, 13);
            this.lblSimGlow.TabIndex = 7;
            this.lblSimGlow.Text = "Glow: 0";
            // 
            // chkSimToggle
            // 
            this.chkSimToggle.Appearance = System.Windows.Forms.Appearance.Button;
            this.chkSimToggle.Checked = true;
            this.chkSimToggle.CheckState = System.Windows.Forms.CheckState.Checked;
            this.chkSimToggle.Font = new System.Drawing.Font("Microsoft Sans Serif", 9.75F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.chkSimToggle.Location = new System.Drawing.Point(6, 285);
            this.chkSimToggle.Name = "chkSimToggle";
            this.chkSimToggle.Size = new System.Drawing.Size(135, 31);
            this.chkSimToggle.TabIndex = 6;
            this.chkSimToggle.Text = "&Pause";
            this.chkSimToggle.TextAlign = System.Drawing.ContentAlignment.MiddleCenter;
            this.chkSimToggle.UseVisualStyleBackColor = true;
            this.chkSimToggle.CheckedChanged += new System.EventHandler(this.chkSimToggle_CheckedChanged);
            // 
            // lblSimDelay
            // 
            this.lblSimDelay.AutoSize = true;
            this.lblSimDelay.Location = new System.Drawing.Point(6, 255);
            this.lblSimDelay.Name = "lblSimDelay";
            this.lblSimDelay.Size = new System.Drawing.Size(46, 13);
            this.lblSimDelay.TabIndex = 5;
            this.lblSimDelay.Text = "Delay: 0";
            // 
            // lblSimFrame
            // 
            this.lblSimFrame.AutoSize = true;
            this.lblSimFrame.Location = new System.Drawing.Point(6, 191);
            this.lblSimFrame.Name = "lblSimFrame";
            this.lblSimFrame.Size = new System.Drawing.Size(39, 13);
            this.lblSimFrame.TabIndex = 4;
            this.lblSimFrame.Text = "Frame:";
            // 
            // tbSimFrames
            // 
            this.tbSimFrames.Enabled = false;
            this.tbSimFrames.LargeChange = 0;
            this.tbSimFrames.Location = new System.Drawing.Point(6, 207);
            this.tbSimFrames.Name = "tbSimFrames";
            this.tbSimFrames.Size = new System.Drawing.Size(135, 45);
            this.tbSimFrames.SmallChange = 0;
            this.tbSimFrames.TabIndex = 3;
            // 
            // btnAddSlope
            // 
            this.btnAddSlope.Location = new System.Drawing.Point(12, 70);
            this.btnAddSlope.Name = "btnAddSlope";
            this.btnAddSlope.Size = new System.Drawing.Size(133, 25);
            this.btnAddSlope.TabIndex = 6;
            this.btnAddSlope.Text = "Generate a gradient";
            this.btnAddSlope.UseVisualStyleBackColor = true;
            this.btnAddSlope.Click += new System.EventHandler(this.btnGenerateGradient_Click);
            // 
            // btnOpenFile
            // 
            this.btnOpenFile.Location = new System.Drawing.Point(12, 12);
            this.btnOpenFile.Name = "btnOpenFile";
            this.btnOpenFile.Size = new System.Drawing.Size(133, 23);
            this.btnOpenFile.TabIndex = 7;
            this.btnOpenFile.Text = "Open from JSON file";
            this.btnOpenFile.UseVisualStyleBackColor = true;
            this.btnOpenFile.Click += new System.EventHandler(this.btnOpenFile_Click);
            // 
            // btnRefresh
            // 
            this.btnRefresh.Location = new System.Drawing.Point(12, 41);
            this.btnRefresh.Name = "btnRefresh";
            this.btnRefresh.Size = new System.Drawing.Size(133, 23);
            this.btnRefresh.TabIndex = 8;
            this.btnRefresh.Text = "Refresh opened file";
            this.btnRefresh.UseVisualStyleBackColor = true;
            this.btnRefresh.Click += new System.EventHandler(this.btnRefresh_Click);
            // 
            // openFileDialog1
            // 
            this.openFileDialog1.DefaultExt = "json";
            this.openFileDialog1.Filter = "JSON files|*.json";
            // 
            // MainForm
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(310, 340);
            this.Controls.Add(this.btnRefresh);
            this.Controls.Add(this.btnOpenFile);
            this.Controls.Add(this.btnAddSlope);
            this.Controls.Add(this.groupBox1);
            this.FormBorderStyle = System.Windows.Forms.FormBorderStyle.FixedSingle;
            this.MaximizeBox = false;
            this.Name = "MainForm";
            this.ShowIcon = false;
            this.Text = "Firefly Abdomen Animation Builder";
            this.panel2.ResumeLayout(false);
            this.groupBox1.ResumeLayout(false);
            this.groupBox1.PerformLayout();
            ((System.ComponentModel.ISupportInitialize)(this.tbSimFrames)).EndInit();
            this.ResumeLayout(false);

        }

        #endregion

        private System.Windows.Forms.Panel panelSimulation;
        private System.Windows.Forms.Timer timerSimulation;
        private System.Windows.Forms.Panel panel2;
        private System.Windows.Forms.GroupBox groupBox1;
        private System.Windows.Forms.TrackBar tbSimFrames;
        private System.Windows.Forms.Label lblSimFrame;
        private System.Windows.Forms.Label lblSimDelay;
        private System.Windows.Forms.CheckBox chkSimToggle;
        private System.Windows.Forms.Label lblSimGlow;
        private System.Windows.Forms.Button btnAddSlope;
        private System.Windows.Forms.Button btnOpenFile;
        private System.Windows.Forms.Button btnRefresh;
        private System.Windows.Forms.OpenFileDialog openFileDialog1;
    }
}

