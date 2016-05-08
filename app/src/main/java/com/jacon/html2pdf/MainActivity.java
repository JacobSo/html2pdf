package com.jacon.html2pdf;

import android.databinding.DataBindingUtil;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorker;
import com.itextpdf.tool.xml.XMLWorkerFontProvider;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.itextpdf.tool.xml.html.CssAppliers;
import com.itextpdf.tool.xml.html.CssAppliersImpl;
import com.itextpdf.tool.xml.html.Tags;
import com.itextpdf.tool.xml.parser.XMLParser;
import com.itextpdf.tool.xml.pipeline.css.CSSResolver;
import com.itextpdf.tool.xml.pipeline.css.CssResolverPipeline;
import com.itextpdf.tool.xml.pipeline.end.PdfWriterPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipelineContext;
import com.jacon.html2pdf.databinding.ActivityMainBinding;

import org.jsoup.Jsoup;
import org.w3c.tidy.Tidy;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        File dir = new File(Environment.getExternalStorageDirectory() + "/html2pdf/");
        if (!dir.exists())
            dir.mkdir();
    }

    public void toInsert(View v) {
        try {
            modify();//插入
            Toast.makeText(this, "toInsert:finish", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void toSwitch(View v) {
        try {
            tidy();//转型
            Toast.makeText(this, "toSwitch:finish", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void toPrint(View v) {
        try {
            print();//输出
            Toast.makeText(this, "toPrint:finish", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * modify source html
     *problem:http://www.ghostsf.com/tag/itext/
     *
     *solution: http://www.open-open.com/jsoup/
     *
     * @throws IOException
     */
    private void modify() throws IOException {
        org.jsoup.nodes.Document doc = Jsoup.parse(getResources().getAssets().open("pdf.html"), "UTF-8", "http://example.com/");
        //doc.getElementById("product");

        File outputFile = new File(Environment.getExternalStorageDirectory() + "/html2pdf/test.html");
        BufferedWriter htmlWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
        htmlWriter.write(doc.html());
        htmlWriter.flush();
        htmlWriter.close();
    }

    /**
     * html2xhtml
     *
     * problem:
     * http://stackoverflow.com/questions/26652029/how-to-do-html-to-xml-conversion-to-generate-closed-tags
     *http://blog.csdn.net/zk_spring/article/details/43412221
     *
     * solution:
     * http://macrotea.iteye.com/blog/1698161
     * @throws IOException
     */
    public void tidy() throws IOException {
        Tidy tidy = new Tidy();
        tidy.setXHTML(true);

        tidy.setHideComments(true);
        tidy.setOutputEncoding("utf-8");
        tidy.setInputEncoding("utf-8");

        File outFile = new File(Environment.getExternalStorageDirectory() + "/html2pdf/temp.html");
        FileOutputStream fos = new FileOutputStream(outFile);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        org.w3c.dom.Document doc = tidy.parseDOM(new FileInputStream(Environment.getExternalStorageDirectory() + "/html2pdf/test.html"), null);
        tidy.pprint(doc, out);
        fos.write(out.toByteArray());
        out.close();
    }

    /**
     * xhtml2pdf
     *
     * solution:
     *http://cctg.blogspot.kr/2013/11/android-app-pdf.html
     * http://developers.itextpdf.com/examples/xml-worker/xml-worker-examples#705-d00_xhtml.java
     * http://www.micmiu.com/opensource/expdoc/itext-html-pdf/
     * @throws Exception
     */

    private static void print() throws Exception {
        MyFontsProvider fontProvider = new MyFontsProvider();
        fontProvider.setUseUnicode(true);//万国编码unicode

        Document document = new Document(PageSize.A4.rotate());
        PdfWriter writer = PdfWriter.getInstance(document,
                new FileOutputStream(Environment.getExternalStorageDirectory() + "/html2pdf/test.pdf"));
        document.open();

        CssAppliers cssAppliers = new CssAppliersImpl(fontProvider);//字体
        CSSResolver cssResolver =
                XMLWorkerHelper.getInstance().getDefaultCssResolver(true);

        HtmlPipelineContext htmlContext = new HtmlPipelineContext(cssAppliers);
        htmlContext.setTagFactory(Tags.getHtmlTagProcessorFactory());
        htmlContext.setImageProvider(new ImageTagProcessor());

        PdfWriterPipeline pdf = new PdfWriterPipeline(document, writer);
        HtmlPipeline htmlPipeline = new HtmlPipeline(htmlContext, pdf);
        CssResolverPipeline css = new CssResolverPipeline(cssResolver, htmlPipeline);

        XMLWorker worker = new XMLWorker(css, true);
        XMLParser p = new XMLParser(worker);
        File html = new File(Environment.getExternalStorageDirectory() + "/html2pdf/temp.html");
        p.parse(new InputStreamReader(new FileInputStream(html), "UTF-8"));
        document.close();

        System.out.println("PDF Created!");
    }


    public static class MyFontsProvider extends XMLWorkerFontProvider {
        public MyFontsProvider() {
            super(null, null);
        }

        @Override
        public Font getFont(final String fontName, String encoding, float size, final int style) {
            return super.getFont("assets/simhei.ttf", "UniGB-UTF8-V", size, style);
        }
    }
}

